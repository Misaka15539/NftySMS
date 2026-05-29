package com.example.smsntfyrelay.presentation

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.smsntfyrelay.domain.AuthType

// ---------------------------------------------------------------------------
// Warning color for SSL disabled card
// ---------------------------------------------------------------------------

private val SslWarningContainerColor = Color(0xFFFFF3E0)   // amber-50
private val SslWarningContentColor = Color(0xFFE65100)     // deep-orange-900

// ---------------------------------------------------------------------------
// SettingsScreen — entry point wired to SettingsViewModel
// ---------------------------------------------------------------------------

@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Refresh status when returning to app (e.g. from settings)
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.refreshReliabilityStatus()
    }

    SettingsScreen(
        uiState = uiState,
        onTopicUrlChanged = viewModel::onTopicUrlChanged,
        onAuthTypeChanged = viewModel::onAuthTypeChanged,
        onBearerTokenChanged = viewModel::onBearerTokenChanged,
        onUsernameChanged = viewModel::onUsernameChanged,
        onPasswordChanged = viewModel::onPasswordChanged,
        onSslValidationChanged = viewModel::onSslValidationChanged,
        onSave = viewModel::save,
        onNavigateBack = onNavigateBack,
    )
}

// ---------------------------------------------------------------------------
// SettingsScreen — stateless composable (testable without ViewModel)
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onTopicUrlChanged: (String) -> Unit,
    onAuthTypeChanged: (AuthType) -> Unit,
    onBearerTokenChanged: (String) -> Unit,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
    onSslValidationChanged: (Boolean) -> Unit,
    onSave: () -> Unit,
    onNavigateBack: () -> Unit,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    // Show "Settings saved" snackbar when saveSuccess becomes true (Req 4.6)
    LaunchedEffect(uiState.saveSuccess) {
        if (uiState.saveSuccess) {
            snackbarHostState.showSnackbar(message = "Settings saved")
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { innerPadding ->

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ----------------------------------------------------------------
            // Topic URL field (Req 4.2, 4.3, 4.4, 4.5)
            // ----------------------------------------------------------------
            TopicUrlSection(
                topicUrl = uiState.topicUrl,
                topicUrlError = uiState.topicUrlError,
                onTopicUrlChanged = onTopicUrlChanged,
            )

            // ----------------------------------------------------------------
            // SSL Validation toggle + warning card (Req 4.7, 4.8, 4.9)
            // ----------------------------------------------------------------
            SslValidationSection(
                sslValidationEnabled = uiState.sslValidationEnabled,
                onSslValidationChanged = onSslValidationChanged,
            )

            // ----------------------------------------------------------------
            // Auth type selector (Req 4.10)
            // ----------------------------------------------------------------
            AuthTypeSection(
                authType = uiState.authType,
                onAuthTypeChanged = onAuthTypeChanged,
            )

            // ----------------------------------------------------------------
            // Auth credential fields (Req 4.11, 4.12, 8.7)
            // ----------------------------------------------------------------
            when (uiState.authType) {
                AuthType.BEARER -> BearerTokenSection(
                    bearerToken = uiState.bearerToken,
                    onBearerTokenChanged = onBearerTokenChanged,
                )
                AuthType.BASIC -> BasicAuthSection(
                    username = uiState.username,
                    password = uiState.password,
                    onUsernameChanged = onUsernameChanged,
                    onPasswordChanged = onPasswordChanged,
                )
                AuthType.NONE -> { /* no credential fields */ }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // ----------------------------------------------------------------
            // Background Reliability section (for MIUI/HyperOS fixes)
            // ----------------------------------------------------------------
            BackgroundReliabilitySection(
                isOptimized = uiState.isBatteryOptimized,
                isXiaomi = uiState.isXiaomiDevice,
                onDisableOptimizations = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                },
                onOpenAppInfo = {
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    context.startActivity(intent)
                }
            )

            // ----------------------------------------------------------------
            // Save button (Req 4.3, 4.6)
            // ----------------------------------------------------------------
            Button(
                onClick = onSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = "Save")
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Topic URL section
// ---------------------------------------------------------------------------

@Composable
private fun TopicUrlSection(
    topicUrl: String,
    topicUrlError: String?,
    onTopicUrlChanged: (String) -> Unit,
) {
    Column {
        Text(
            text = "ntfy Topic URL",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = topicUrl,
            onValueChange = onTopicUrlChanged,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Topic URL") },
            placeholder = { Text("https://ntfy.sh/my-topic") },
            isError = topicUrlError != null,
            supportingText = if (topicUrlError != null) {
                { Text(text = topicUrlError, color = MaterialTheme.colorScheme.error) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Uri,
            ),
        )
    }
}

// ---------------------------------------------------------------------------
// SSL Validation section
// ---------------------------------------------------------------------------

@Composable
private fun SslValidationSection(
    sslValidationEnabled: Boolean,
    onSslValidationChanged: (Boolean) -> Unit,
) {
    Column {
        Text(
            text = "Connection Security",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
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
                        text = "Validate SSL Certificate",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Text(
                        text = "Verify the server's TLS certificate",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = sslValidationEnabled,
                    onCheckedChange = onSslValidationChanged,
                )
            }
        }

        // Prominent warning card shown when SSL validation is disabled (Req 4.9)
        if (!sslValidationEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            SslWarningCard()
        }
    }
}

// ---------------------------------------------------------------------------
// SSL warning card
// ---------------------------------------------------------------------------

@Composable
private fun SslWarningCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = SslWarningContainerColor,
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = "Warning",
                tint = SslWarningContentColor,
                modifier = Modifier.size(24.dp),
            )
            Text(
                text = "Warning: Disabling SSL validation exposes your connection to " +
                    "interception. Your credentials may be transmitted insecurely.",
                style = MaterialTheme.typography.bodySmall,
                color = SslWarningContentColor,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Auth type section
// ---------------------------------------------------------------------------

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthTypeSection(
    authType: AuthType,
    onAuthTypeChanged: (AuthType) -> Unit,
) {
    val authOptions = listOf(AuthType.NONE, AuthType.BEARER, AuthType.BASIC)
    val authLabels = mapOf(
        AuthType.NONE to "None",
        AuthType.BEARER to "Bearer",
        AuthType.BASIC to "Basic",
    )

    Column {
        Text(
            text = "Authorization",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(8.dp))
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
            authOptions.forEachIndexed { index, option ->
                SegmentedButton(
                    selected = authType == option,
                    onClick = { onAuthTypeChanged(option) },
                    shape = SegmentedButtonDefaults.itemShape(
                        index = index,
                        count = authOptions.size,
                    ),
                    label = { Text(authLabels[option] ?: option.name) },
                )
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Bearer token section (Req 4.11, 8.7)
// ---------------------------------------------------------------------------

@Composable
private fun BearerTokenSection(
    bearerToken: String,
    onBearerTokenChanged: (String) -> Unit,
) {
    // Credential masking rule (Req 8.7):
    // If a token is already saved (bearerToken is non-empty on initial load),
    // show ••••••••  as placeholder but do NOT pre-populate the field.
    // The user must type a new value to change it.
    val hasSavedToken = bearerToken.isNotEmpty()

    // Track whether the user has started editing this session
    var userHasEdited by rememberSaveable { mutableStateOf(false) }
    var fieldValue by rememberSaveable { mutableStateOf("") }

    Column {
        Text(
            text = "Bearer Token",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { newValue ->
                fieldValue = newValue
                userHasEdited = true
                onBearerTokenChanged(newValue)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Token") },
            placeholder = {
                Text(
                    text = if (hasSavedToken && !userHasEdited) "••••••••" else "Enter bearer token",
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
            ),
            singleLine = true,
        )
        if (hasSavedToken && !userHasEdited) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "A token is already saved. Enter a new value to replace it.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Basic auth section (Req 4.12, 8.7)
// ---------------------------------------------------------------------------

@Composable
private fun BasicAuthSection(
    username: String,
    password: String,
    onUsernameChanged: (String) -> Unit,
    onPasswordChanged: (String) -> Unit,
) {
    val hasSavedUsername = username.isNotEmpty()
    val hasSavedPassword = password.isNotEmpty()

    var usernameEdited by rememberSaveable { mutableStateOf(false) }
    var passwordEdited by rememberSaveable { mutableStateOf(false) }
    var usernameField by rememberSaveable { mutableStateOf("") }
    var passwordField by rememberSaveable { mutableStateOf("") }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = "Basic Auth Credentials",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )

        // Username field
        OutlinedTextField(
            value = usernameField,
            onValueChange = { newValue ->
                usernameField = newValue
                usernameEdited = true
                onUsernameChanged(newValue)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Username") },
            placeholder = {
                Text(
                    text = if (hasSavedUsername && !usernameEdited) "••••••••" else "Enter username",
                )
            },
            singleLine = true,
        )

        // Password field
        OutlinedTextField(
            value = passwordField,
            onValueChange = { newValue ->
                passwordField = newValue
                passwordEdited = true
                onPasswordChanged(newValue)
            },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Password") },
            placeholder = {
                Text(
                    text = if (hasSavedPassword && !passwordEdited) "••••••••" else "Enter password",
                )
            },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                keyboardType = KeyboardType.Password,
            ),
            singleLine = true,
        )

        if ((hasSavedUsername && !usernameEdited) || (hasSavedPassword && !passwordEdited)) {
            Text(
                text = "Credentials are already saved. Enter new values to replace them.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Background Reliability section (Address MIUI/HyperOS issues)
// ---------------------------------------------------------------------------

@Composable
private fun BackgroundReliabilitySection(
    isOptimized: Boolean,
    isXiaomi: Boolean,
    onDisableOptimizations: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    Column {
        Text(
            text = "Background Reliability",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(modifier = Modifier.height(4.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (isOptimized) Icons.Default.Warning else Icons.Default.Info,
                        contentDescription = null,
                        tint = if (isOptimized) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Text(
                        text = if (isOptimized) "Battery optimization is active" else "Battery optimization is disabled",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                        color = if (isOptimized) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Text(
                    text = "To ensure SMS are relayed instantly, the app should be allowed to run in the background without restrictions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (isOptimized) {
                    OutlinedButton(
                        onClick = onDisableOptimizations,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Disable Battery Optimizations")
                    }
                }

                if (isXiaomi) {
                    Button(
                        onClick = onOpenAppInfo,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary
                        )
                    ) {
                        Text("Fix for MIUI/HyperOS (Auto-start)")
                    }
                    Text(
                        text = "Note: On MIUI/HyperOS, please enable 'Auto-start' and set 'Battery saver' to 'No restrictions' in the App Info page.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
