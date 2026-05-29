package com.example.smsntfyrelay.domain

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.example.smsntfyrelay.data.LogEntry
import com.example.smsntfyrelay.data.LogRepository
import com.example.smsntfyrelay.data.CredentialRepository
import com.example.smsntfyrelay.data.NtfyRepository
import com.example.smsntfyrelay.data.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * Orchestrates the full SMS relay pipeline:
 *   1. Check relay toggle
 *   2. Check topic URL is configured
 *   3. Check network connectivity
 *   4. Read auth credentials and SSL setting
 *   5. POST to ntfy server
 *   6. Log the outcome
 *   7. Optionally log an SSL_WARNING when credentials are sent over an unvalidated connection
 */
class RelayUseCase @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val credentialRepository: CredentialRepository,
    private val ntfyRepository: NtfyRepository,
    private val logRepository: LogRepository,
) {

    suspend fun relay(sender: String, body: String, logId: Long? = null): Boolean {
        // 1. Check relay toggle — bail out early if disabled
        val relayEnabled = settingsRepository.relayEnabled.first()
        if (!relayEnabled) {
            if (logId != null) {
                logRepository.updateStatus(logId, LogOutcome.RELAY_DISABLED)
            } else {
                logRepository.insert(
                    LogEntry(
                        timestampMs = System.currentTimeMillis(),
                        sender = sender,
                        messagePreview = body.take(60),
                        outcome = LogOutcome.RELAY_DISABLED,
                    ),
                )
            }
            return true
        }

        return performRelay(sender, body, logId)
    }

    /**
     * Tests the connection to the configured ntfy server by sending a "triggered" message.
     * @return true if the test was successful, false otherwise.
     */
    suspend fun testConnection(): Boolean {
        val topicUrl = settingsRepository.topicUrl.first()
        if (topicUrl.isBlank()) return false

        // 1. Check network connectivity
        if (!isNetworkAvailable()) return false

        // 2. Read auth credentials
        val authConfig: AuthConfig = credentialRepository.getAuthConfig() ?: AuthConfig.None

        // 3. Read SSL validation setting
        val sslValidationEnabled = settingsRepository.sslValidationEnabled.first()

        // 4. POST "triggered" message to ntfy server
        val result = ntfyRepository.post(
            topicUrl = topicUrl,
            title = "Relay Connection Test",
            body = "SMS Relay connection test successful",
            auth = authConfig,
            sslValidation = sslValidationEnabled,
        )

        // 5. Map RelayResult → LogOutcome and insert the log entry
        val (outcome, detail) = result.toLogOutcomeAndDetail()
        val finalOutcome = if (outcome == LogOutcome.SUCCESS) LogOutcome.TEST_CONNECTION else outcome

        logRepository.insert(
            LogEntry(
                timestampMs = System.currentTimeMillis(),
                sender = "System",
                messagePreview = "Connection test",
                outcome = finalOutcome,
                detail = detail,
            )
        )

        return outcome == LogOutcome.SUCCESS
    }

    private suspend fun performRelay(sender: String, body: String, logId: Long?): Boolean {
        // 2. Check topic URL — bail out if not configured
        val topicUrl = settingsRepository.topicUrl.first()
        if (topicUrl.isBlank()) {
            if (logId != null) {
                logRepository.updateStatus(logId, LogOutcome.MISSING_CONFIG)
            } else {
                logRepository.insert(
                    LogEntry(
                        timestampMs = System.currentTimeMillis(),
                        sender = sender,
                        messagePreview = body.take(60),
                        outcome = LogOutcome.MISSING_CONFIG,
                    )
                )
            }
            return true // Config error is permanent, don't retry
        }

        // 3. Check network connectivity
        if (!isNetworkAvailable()) {
            if (logId != null) {
                logRepository.updateStatus(logId, LogOutcome.NETWORK_UNAVAILABLE)
            } else {
                logRepository.insert(
                    LogEntry(
                        timestampMs = System.currentTimeMillis(),
                        sender = sender,
                        messagePreview = body.take(60),
                        outcome = LogOutcome.NETWORK_UNAVAILABLE,
                    )
                )
            }
            return false // Transient error, should retry
        }

        // 4. Read auth credentials (synchronous read, already on IO dispatcher via SmsReceiver)
        val authConfig: AuthConfig = credentialRepository.getAuthConfig() ?: AuthConfig.None

        // 5. Read SSL validation setting
        val sslValidationEnabled = settingsRepository.sslValidationEnabled.first()

        // 6. POST to ntfy server
        val result = ntfyRepository.post(
            topicUrl = topicUrl,
            title = "SMS from $sender",
            body = body,
            auth = authConfig,
            sslValidation = sslValidationEnabled,
        )

        // 7. Map RelayResult → LogOutcome and insert/update the log entry
        val (outcome, detail) = result.toLogOutcomeAndDetail()
        if (logId != null) {
            logRepository.updateStatus(logId, outcome, detail)
        } else {
            logRepository.insert(
                LogEntry(
                    timestampMs = System.currentTimeMillis(),
                    sender = sender,
                    messagePreview = body.take(60),
                    outcome = outcome,
                    detail = detail,
                )
            )
        }

        // 8. If SSL validation is disabled and credentials are present, add an SSL_WARNING entry
        if (!sslValidationEnabled && (authConfig !is AuthConfig.None)) {
            logRepository.insert(
                LogEntry(
                    timestampMs = System.currentTimeMillis(),
                    sender = sender,
                    messagePreview = body.take(60),
                    outcome = LogOutcome.SSL_WARNING,
                    detail = "Authorization header sent over a connection with SSL validation disabled",
                )
            )
        }

        return outcome == LogOutcome.SUCCESS
    }

    // -----------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    /**
     * Converts a [RelayResult] to a [LogOutcome] + optional detail string pair.
     */
    private fun RelayResult.toLogOutcomeAndDetail(): Pair<LogOutcome, String?> = when (this) {
        is RelayResult.Success -> LogOutcome.SUCCESS to "HTTP $httpStatus"
        is RelayResult.HttpError -> LogOutcome.HTTP_ERROR to "HTTP $httpStatus"
        is RelayResult.Timeout -> LogOutcome.TIMEOUT to null
        is RelayResult.NetworkUnavailable -> LogOutcome.NETWORK_UNAVAILABLE to null
        is RelayResult.MissingConfig -> LogOutcome.MISSING_CONFIG to null
    }
}
