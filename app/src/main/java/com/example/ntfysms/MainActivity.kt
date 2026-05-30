package com.example.ntfysms

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ntfysms.presentation.MainScreen
import com.example.ntfysms.presentation.SettingsScreen
import com.example.ntfysms.ui.theme.NtfySMSTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NtfySMSTheme {
                val context = LocalContext.current

                // Track whether required permissions are currently granted.
                var permissionsGranted by remember {
                    mutableStateOf(
                        ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.RECEIVE_SMS,
                        ) == PackageManager.PERMISSION_GRANTED &&
                                ContextCompat.checkSelfPermission(
                                    context,
                                    Manifest.permission.READ_SMS,
                                ) == PackageManager.PERMISSION_GRANTED,
                    )
                }

                // Launcher that presents the runtime permission dialog and updates state
                // when the user responds.
                val permissionLauncher = rememberLauncherForActivityResult(
                    contract = ActivityResultContracts.RequestMultiplePermissions(),
                ) { grants ->
                    permissionsGranted = grants.values.all { it }
                }

                // On first composition, automatically request the permission if it has
                // not been granted yet.
                LaunchedEffect(Unit) {
                    if (!permissionsGranted) {
                        permissionLauncher.launch(
                            arrayOf(
                                Manifest.permission.RECEIVE_SMS,
                                Manifest.permission.READ_SMS,
                            ),
                        )
                    }
                }

                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "main",
                ) {
                    composable("main") {
                        MainScreen(
                            smsPermissionGranted = permissionsGranted,
                            onNavigateToSettings = { navController.navigate("settings") },
                        )
                    }
                    composable("settings") {
                        SettingsScreen(
                            onNavigateBack = { navController.popBackStack() },
                        )
                    }
                }
            }
        }
    }
}
