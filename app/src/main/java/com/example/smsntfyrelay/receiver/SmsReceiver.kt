package com.example.smsntfyrelay.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.SmsMessage
import android.util.Log
import com.example.smsntfyrelay.BuildConfig
import com.example.smsntfyrelay.data.LogEntry
import com.example.smsntfyrelay.data.LogRepository
import com.example.smsntfyrelay.domain.LogOutcome
import com.example.smsntfyrelay.domain.RelayUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val TAG = "SmsReceiver"

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {

    @Inject
    lateinit var relayUseCase: RelayUseCase

    @Inject
    lateinit var logRepository: LogRepository

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult: PendingResult = goAsync()

        // Parse PDUs before launching the coroutine so we can catch parse errors synchronously
        val sender: String
        val body: String

        try {
            val format = intent.getStringExtra("format")
            val extras = intent.extras
                ?: throw IllegalArgumentException("Intent extras are null")

            @Suppress("DEPRECATION")
            val pdus = extras.get("pdus") as? Array<*>
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

            // Insert PARSE_ERROR log entry without crashing
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    logRepository.insert(
                        LogEntry(
                            timestampMs = System.currentTimeMillis(),
                            sender = "unknown",
                            messagePreview = "",
                            outcome = LogOutcome.PARSE_ERROR,
                            detail = sanitisedMessage,
                        )
                    )
                } finally {
                    pendingResult.finish()
                }
            }
            return
        }

        // Relay the parsed SMS on an IO coroutine; always finish the PendingResult
        CoroutineScope(Dispatchers.IO).launch {
            try {
                relayUseCase.relay(sender, body)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
