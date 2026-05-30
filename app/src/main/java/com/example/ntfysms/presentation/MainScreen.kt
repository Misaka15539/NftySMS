package com.example.ntfysms.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ntfysms.R
import com.example.ntfysms.data.LogEntry
import com.example.ntfysms.domain.LogOutcome
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ---------------------------------------------------------------------------
// Outcome chip colors
// ---------------------------------------------------------------------------

private val OutcomeSuccess = Color(0xFF2E7D32)       // green
private val OutcomePending = Color(0xFF1976D2)       // blue
private val OutcomeError = Color(0xFFC62828)         // red
private val OutcomeDisabled = Color(0xFF757575)      // gray
private val OutcomeSslWarning = Color(0xFFF57F17)    // orange/yellow

private fun LogOutcome.chipColor(): Color = when (this) {
    LogOutcome.SUCCESS,
    LogOutcome.TEST_CONNECTION -> OutcomeSuccess
    LogOutcome.PENDING -> OutcomePending
    LogOutcome.HTTP_ERROR,
    LogOutcome.TIMEOUT,
    LogOutcome.NETWORK_UNAVAILABLE,
    LogOutcome.MISSING_CONFIG,
    LogOutcome.PARSE_ERROR -> OutcomeError
    LogOutcome.RELAY_DISABLED -> OutcomeDisabled
    LogOutcome.SSL_WARNING -> OutcomeSslWarning
}

private fun LogOutcome.labelResId(): Int = when (this) {
    LogOutcome.SUCCESS -> R.string.outcome_success
    LogOutcome.PENDING -> R.string.outcome_pending
    LogOutcome.HTTP_ERROR -> R.string.outcome_http_error
    LogOutcome.TIMEOUT -> R.string.outcome_timeout
    LogOutcome.NETWORK_UNAVAILABLE -> R.string.outcome_no_network
    LogOutcome.MISSING_CONFIG -> R.string.outcome_missing_config
    LogOutcome.RELAY_DISABLED -> R.string.outcome_relay_disabled
    LogOutcome.PARSE_ERROR -> R.string.outcome_parse_error
    LogOutcome.SSL_WARNING -> R.string.outcome_ssl_warning
    LogOutcome.TEST_CONNECTION -> R.string.outcome_connection_test
}

// ---------------------------------------------------------------------------
// MainScreen
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onNavigateToSettings: () -> Unit,
    smsPermissionGranted: Boolean,
    viewModel: MainViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // Show Snackbar when SMS permission is denied and user tries to enable relay
    val smsPermissionRequiredMsg = stringResource(R.string.msg_sms_permission_required)
    LaunchedEffect(smsPermissionGranted) {
        if (!smsPermissionGranted) {
            snackbarHostState.showSnackbar(
                message = smsPermissionRequiredMsg,
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = stringResource(id = R.string.app_name),
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                actions = {
                    // Clear Log action
                    IconButton(onClick = { showClearConfirmDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.action_clear_log),
                        )
                    }
                    // Navigate to Settings
                    IconButton(onClick = onNavigateToSettings) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = stringResource(R.string.action_settings),
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            // ----------------------------------------------------------------
            // Relay toggle section
            // ----------------------------------------------------------------
            val urlInvalidMsg = stringResource(R.string.msg_configure_url_first)
            val connectionFailedMsg = stringResource(R.string.msg_connection_test_failed)
            RelayToggleSection(
                relayEnabled = uiState.relayEnabled,
                smsPermissionGranted = smsPermissionGranted,
                isTestingConnection = uiState.isTestingConnection,
                onToggleChanged = { enabled ->
                    if (!smsPermissionGranted) {
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(
                                message = smsPermissionRequiredMsg,
                            )
                        }
                    } else {
                        viewModel.setRelayEnabled(
                            enabled = enabled,
                            onUrlInvalid = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = urlInvalidMsg,
                                    )
                                }
                            },
                            onConnectionFailed = {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar(
                                        message = connectionFailedMsg,
                                    )
                                }
                            }
                        )
                    }
                },
            )

            // ----------------------------------------------------------------
            // Activity log section
            // ----------------------------------------------------------------
            if (uiState.logEntries.isEmpty()) {
                EmptyLogState(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                )
            } else {
                LogList(
                    entries = uiState.logEntries,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    // ----------------------------------------------------------------
    // Clear Log confirmation dialog
    // ----------------------------------------------------------------
    if (showClearConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text(stringResource(R.string.dialog_clear_log_title)) },
            text = { Text(stringResource(R.string.dialog_clear_log_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearLog()
                        showClearConfirmDialog = false
                    },
                ) {
                    Text(stringResource(R.string.dialog_action_clear), color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text(stringResource(R.string.dialog_action_cancel))
                }
            },
        )
    }
}

// ---------------------------------------------------------------------------
// Relay toggle section
// ---------------------------------------------------------------------------

@Composable
private fun RelayToggleSection(
    relayEnabled: Boolean,
    smsPermissionGranted: Boolean,
    isTestingConnection: Boolean,
    onToggleChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.relay_toggle_title),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (!smsPermissionGranted) {
                        stringResource(R.string.status_sms_permission_required)
                    } else if (isTestingConnection) {
                        stringResource(R.string.status_testing_connection)
                    } else if (relayEnabled) {
                        stringResource(R.string.status_forwarding)
                    } else {
                        stringResource(R.string.status_paused)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = relayEnabled,
                onCheckedChange = onToggleChanged,
                enabled = smsPermissionGranted && !isTestingConnection,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Log list
// ---------------------------------------------------------------------------

@Composable
private fun LogList(
    entries: List<LogEntry>,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Scroll to top (most recent entry) when a new entry is added.
    // Entries are ordered DESC by timestamp so index 0 is the newest.
    LaunchedEffect(entries.size) {
        if (entries.isNotEmpty()) {
            listState.animateScrollToItem(0)
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(
            items = entries,
            key = { it.id },
        ) { entry ->
            LogEntryCard(entry = entry)
        }
    }
}

// ---------------------------------------------------------------------------
// Individual log entry card
// ---------------------------------------------------------------------------

@Composable
private fun LogEntryCard(
    entry: LogEntry,
    modifier: Modifier = Modifier,
) {
    val dateFormatter = remember {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            // Top row: timestamp + outcome chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = dateFormatter.format(Date(entry.timestampMs)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutcomeChip(outcome = entry.outcome)
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Sender
            Text(
                text = entry.sender,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
            )

            Spacer(modifier = Modifier.height(2.dp))

            // Message preview
            Text(
                text = entry.messagePreview,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )

            // Optional detail (HTTP status, error message, etc.)
            if (!entry.detail.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = entry.detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Outcome chip / badge
// ---------------------------------------------------------------------------

@Composable
private fun OutcomeChip(
    outcome: LogOutcome,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = outcome.chipColor(),
    ) {
        Text(
            text = stringResource(outcome.labelResId()),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = Color.White,
        )
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun EmptyLogState(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            // Simple icon placeholder for the empty state illustration
            Surface(
                modifier = Modifier.size(72.dp),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "📭",
                        style = MaterialTheme.typography.headlineLarge,
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(R.string.empty_log_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.empty_log_description),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 32.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}
