package com.example.smsntfyrelay.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.smsntfyrelay.data.LogEntry
import com.example.smsntfyrelay.data.LogRepository
import com.example.smsntfyrelay.data.SettingsRepository
import com.example.smsntfyrelay.domain.RelayUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

// ---------------------------------------------------------------------------
// MainUiState — snapshot of all data the main screen needs to render
// ---------------------------------------------------------------------------

data class MainUiState(
    val relayEnabled: Boolean = false,
    val logEntries: List<LogEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isTestingConnection: Boolean = false,
)

// ---------------------------------------------------------------------------
// MainViewModel — drives the main screen UI
// ---------------------------------------------------------------------------

@HiltViewModel
class MainViewModel @Inject constructor(
    private val logRepository: LogRepository,
    private val settingsRepository: SettingsRepository,
    private val relayUseCase: RelayUseCase,
) : ViewModel() {

    private val _isTestingConnection = MutableStateFlow(false)

    /**
     * Combined state flow that merges relay-enabled setting and log entries.
     * Starts with [MainUiState.isLoading] = true; flips to false on first emission.
     */
    val uiState: StateFlow<MainUiState> =
        combine(
            settingsRepository.relayEnabled,
            logRepository.observeEntries(),
            _isTestingConnection,
        ) { relayEnabled, logEntries, isTesting ->
            MainUiState(
                relayEnabled = relayEnabled,
                logEntries = logEntries,
                isLoading = false,
                isTestingConnection = isTesting,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = MainUiState(isLoading = true),
        )

    /** Enable or disable SMS relaying. */
    fun setRelayEnabled(enabled: Boolean, onUrlInvalid: () -> Unit, onConnectionFailed: () -> Unit) {
        viewModelScope.launch {
            if (enabled) {
                val topicUrl = settingsRepository.topicUrl.first()
                if (topicUrl.isBlank()) {
                    onUrlInvalid()
                    return@launch
                }

                _isTestingConnection.value = true
                val success = relayUseCase.testConnection()
                _isTestingConnection.value = false

                if (!success) {
                    onConnectionFailed()
                    return@launch
                }
            }
            settingsRepository.setRelayEnabled(enabled)
        }
    }

    /** Clear all log entries. */
    fun clearLog() {
        viewModelScope.launch {
            logRepository.clearAll()
        }
    }
}
