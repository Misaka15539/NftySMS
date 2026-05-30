package com.example.ntfysms.presentation

import android.content.Context
import android.os.PowerManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ntfysms.data.CredentialRepository
import com.example.ntfysms.data.SettingsRepository
import com.example.ntfysms.domain.AuthConfig
import com.example.ntfysms.domain.AuthType
import com.example.ntfysms.domain.SettingsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------
// SettingsUiState — snapshot of all data the settings screen needs to render
// ---------------------------------------------------------------------------

data class SettingsUiState(
    val topicUrl: String = "",
    val topicUrlError: String? = null,
    val sslValidationEnabled: Boolean = true,
    val authType: AuthType = AuthType.NONE,
    val bearerToken: String = "",
    val username: String = "",
    val password: String = "",
    val saveSuccess: Boolean = false,
    val isBatteryOptimized: Boolean = true,
    val isXiaomiDevice: Boolean = false,
)

// ---------------------------------------------------------------------------
// SettingsViewModel — drives the settings screen UI
// ---------------------------------------------------------------------------

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val credentialRepository: CredentialRepository,
    private val settingsUseCase: SettingsUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            // Load persisted settings to pre-populate the form
            val topicUrl = settingsRepository.topicUrl.first()
            val sslValidationEnabled = settingsRepository.sslValidationEnabled.first()
            val authConfig = credentialRepository.getAuthConfig()

            val (authType, bearerToken, username, password) = when (authConfig) {
                is AuthConfig.Bearer -> Quadruple(AuthType.BEARER, authConfig.token, "", "")
                is AuthConfig.Basic -> Quadruple(AuthType.BASIC, "", authConfig.username, authConfig.password)
                else -> Quadruple(AuthType.NONE, "", "", "")
            }

            _uiState.update { current ->
                current.copy(
                    topicUrl = topicUrl,
                    sslValidationEnabled = sslValidationEnabled,
                    authType = authType,
                    bearerToken = bearerToken,
                    username = username,
                    password = password,
                    isBatteryOptimized = !isIgnoringBatteryOptimizations(),
                    isXiaomiDevice = isXiaomiDevice(),
                )
            }
        }
    }

    /** Re-checks the battery optimization status (e.g. when returning to screen). */
    fun refreshReliabilityStatus() {
        _uiState.update { it.copy(isBatteryOptimized = !isIgnoringBatteryOptimizations()) }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun isXiaomiDevice(): Boolean {
        val manufacturer = android.os.Build.MANUFACTURER
        return manufacturer.equals("Xiaomi", ignoreCase = true)
    }

    // -----------------------------------------------------------------------
    // Field change handlers
    // -----------------------------------------------------------------------

    /** Called on every keystroke in the topic URL field. Validates immediately. */
    fun onTopicUrlChanged(url: String) {
        _uiState.update { current ->
            current.copy(
                topicUrl = url,
                topicUrlError = settingsUseCase.validateTopicUrl(url),
                saveSuccess = false,
            )
        }
    }

    /** Called when the user selects a different auth type. */
    fun onAuthTypeChanged(authType: AuthType) {
        _uiState.update { current ->
            current.copy(
                authType = authType,
                saveSuccess = false,
            )
        }
    }

    /** Called when the user toggles SSL validation. */
    fun onSslValidationChanged(enabled: Boolean) {
        _uiState.update { current ->
            current.copy(
                sslValidationEnabled = enabled,
                saveSuccess = false,
            )
        }
    }

    /** Called when the user edits the bearer token field. */
    fun onBearerTokenChanged(token: String) {
        _uiState.update { current ->
            current.copy(
                bearerToken = token,
                saveSuccess = false,
            )
        }
    }

    /** Called when the user edits the username field. */
    fun onUsernameChanged(username: String) {
        _uiState.update { current ->
            current.copy(
                username = username,
                saveSuccess = false,
            )
        }
    }

    /** Called when the user edits the password field. */
    fun onPasswordChanged(password: String) {
        _uiState.update { current ->
            current.copy(
                password = password,
                saveSuccess = false,
            )
        }
    }

    // -----------------------------------------------------------------------
    // Save
    // -----------------------------------------------------------------------

    /**
     * Validate and persist all settings.
     *
     * - If the URL is invalid, sets [SettingsUiState.topicUrlError] and returns early.
     * - Persists topic URL and SSL validation flag via [SettingsRepository].
     * - Persists or clears [AuthConfig] via [CredentialRepository].
     * - Sets [SettingsUiState.saveSuccess] = true on success.
     */
    fun save() {
        val state = _uiState.value
        val urlError = settingsUseCase.validateTopicUrl(state.topicUrl)
        if (urlError != null) {
            _uiState.update { it.copy(topicUrlError = urlError) }
            return
        }

        viewModelScope.launch {
            // Persist topic URL and SSL validation setting
            settingsRepository.saveTopicUrl(state.topicUrl)
            settingsRepository.setSslValidationEnabled(state.sslValidationEnabled)

            // Persist or clear auth credentials
            when (state.authType) {
                AuthType.NONE -> credentialRepository.clearAuthConfig()
                AuthType.BEARER -> credentialRepository.saveAuthConfig(
                    AuthConfig.Bearer(state.bearerToken)
                )
                AuthType.BASIC -> credentialRepository.saveAuthConfig(
                    AuthConfig.Basic(state.username, state.password)
                )
            }

            _uiState.update { it.copy(saveSuccess = true) }
        }
    }
}

// ---------------------------------------------------------------------------
// Private helper — destructuring for 4-tuple from init block
// ---------------------------------------------------------------------------

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
