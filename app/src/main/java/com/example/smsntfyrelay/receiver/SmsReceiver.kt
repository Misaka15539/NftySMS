package com.example.smsntfyrelay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import com.example.smsntfyrelay.BuildConfig
import com.example.smsntfyrelay.data.LogEntry
import com.example.smsntfyrelay.data.LogRepository
import com.example.smsntfyrelay.domain.LogOutcome
import com.example.smsntfyrelay.worker.RelayWorker
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SmsReceiver"

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var logRepository: LogRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val pendingResult = goAsync()

        // Parse PDUs before launching the coroutine or worker
        val sender: String
        val body: String

        try {
            val format = intent.getStringExtra("format")
            val extras = intent.extras
                ?: throw IllegalArgumentException("Intent extras are null")

            @Suppress("DEPRECATION")
            val pdus = (extras["pdus"] as? Array<*>)
                ?: throw IllegalArgumentException("No PDUs found in intent extras")

            val messages = pdus.map { pdu ->
                SmsMessage.createFromPdu(pdu as ByteArray, format)
                    ?: throw IllegalArgumentException("SmsMessage.createFromPdu returned null")
            }

            if (messages.isEmpty()) {
                throw IllegalArgumentException("PDU array is empty")
            }

            sender = messages.first().originatingAddress
                ?: throw IllegalArgumentException("Originating address is null")

            body = messages.joinToString(separator = "") { it.messageBody ?: "" }
        } catch (e: Exception) {
            val sanitisedMessage = if (BuildConfig.DEBUG) {
                e.message ?: "Unknown parse error"
            } else {
                "SMS PDU parse error"
            }
            Log.e(TAG, "Failed to parse SMS PDUs", e)

            // Insert PARSE_ERROR log entry
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    logRepository.insert(
                        LogEntry(
                            timestampMs = System.currentTimeMillis(),
                            sender = "unknown",
                            messagePreview = "",
                            outcome = LogOutcome.PARSE_ERROR,
                            detail = sanitisedMessage,
                        ),
                    )
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // Enqueue expedited work to relay the SMS
        try {
            // Insert PENDING log entry immediately
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val logId = logRepository.insert(
                        LogEntry(
                            timestampMs = System.currentTimeMillis(),
                            sender = sender,
                            messagePreview = body.take(60),
                            outcome = LogOutcome.PENDING,
                            detail = "Waiting for internet..."
                        )
                    )

                    val workData = Data.Builder()
                        .putString(RelayWorker.KEY_SENDER, sender)
                        .putString(RelayWorker.KEY_BODY, body)
                        .putLong(RelayWorker.KEY_LOG_ID, logId)
                        .build()

                    val constraints = Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()

                    val workRequest = OneTimeWorkRequestBuilder<RelayWorker>()
                        .setConstraints(constraints)
                        .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                        .setInputData(workData)
                        .build()

                    WorkManager.getInstance(context).enqueue(workRequest)
                } finally {
                    pendingResult.finish()
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "onReceive: failed to enqueue work", e)
            pendingResult.finish()
        }
    }
}
