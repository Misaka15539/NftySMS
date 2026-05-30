package com.example.ntfysms.receiver

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
import com.example.ntfysms.BuildConfig
import com.example.ntfysms.R
import com.example.ntfysms.data.LogEntry
import com.example.ntfysms.data.LogRepository
import com.example.ntfysms.domain.LogOutcome
import com.example.ntfysms.worker.RelayWorker
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
                e.message ?: context.getString(R.string.log_detail_unknown_parse_error)
            } else {
                context.getString(R.string.log_detail_parse_error)
            }
            Log.e(TAG, "Failed to parse SMS PDUs", e)

            // Insert PARSE_ERROR log entry
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    logRepository.insert(
                        LogEntry(
                            timestampMs = System.currentTimeMillis(),
                            sender = context.getString(R.string.relay_sender_unknown),
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
            val pendingMsg = context.getString(R.string.log_detail_pending)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val logId = logRepository.insert(
                        LogEntry(
                            timestampMs = System.currentTimeMillis(),
                            sender = sender,
                            messagePreview = body.take(60),
                            outcome = LogOutcome.PENDING,
                            detail = pendingMsg,
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
