package com.example.smsntfyrelay.domain

import com.example.smsntfyrelay.data.SettingsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case exposing settings read/write operations plus URL validation logic.
 * Thin wrapper over [SettingsRepository] for use by ViewModels.
 */
class SettingsUseCase @Inject constructor(
    private val settingsRepository: SettingsRepository,
) {

    // -----------------------------------------------------------------------
    // Read operations — delegate directly to repository
    // -----------------------------------------------------------------------

    /** Emits the currently saved ntfy topic URL; defaults to empty string. */
    val topicUrl: Flow<String> get() = settingsRepository.topicUrl

    /** Emits whether SMS relaying is enabled; defaults to false. */
    val relayEnabled: Flow<Boolean> get() = settingsRepository.relayEnabled

    /** Emits whether SSL/TLS certificate validation is enabled; defaults to true. */
    val sslValidationEnabled: Flow<Boolean> get() = settingsRepository.sslValidationEnabled

    // -----------------------------------------------------------------------
    // Write operations — delegate directly to repository (with validation guard)
    // -----------------------------------------------------------------------

    /**
     * Validate and persist a new topic URL.
     * If [validateTopicUrl] returns a non-null error, the URL is NOT saved and
     * this function returns immediately without writing to storage.
     *
     * Callers (e.g. ViewModels) should also call [validateTopicUrl] on every
     * keystroke to surface inline errors before the user taps Save.
     */
    suspend fun saveTopicUrl(url: String) {
        if (validateTopicUrl(url) != null) return
        settingsRepository.saveTopicUrl(url)
    }

    /** Enable or disable SMS relaying. */
    suspend fun setRelayEnabled(enabled: Boolean) = settingsRepository.setRelayEnabled(enabled)

    /** Enable or disable SSL/TLS certificate validation. */
    suspend fun setSslValidationEnabled(enabled: Boolean) =
        settingsRepository.setSslValidationEnabled(enabled)

    // -----------------------------------------------------------------------
    // URL validation
    // -----------------------------------------------------------------------

    /**
     * Validate a topic URL string.
     *
     * @return a non-null error message if the URL is invalid, or null if valid.
     *
     * Rules (Requirements 4.4, 4.5):
     * - Empty string → "URL is required"
     * - Does not start with `http://` or `https://` → "URL must start with http:// or https://"
     * - Otherwise → null (valid)
     */
    fun validateTopicUrl(url: String): String? {
        if (url.isEmpty()) return "URL is required"
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            return "URL must start with http:// or https://"
        }
        return null
    }
}
