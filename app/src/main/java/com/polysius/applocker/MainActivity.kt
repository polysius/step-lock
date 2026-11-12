package com.polysius.applocker

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polysius.applocker.permissions.PermissionKind
import com.polysius.applocker.ui.HomeScreen
import com.polysius.applocker.ui.MainViewModel
import com.polysius.applocker.ui.Screen
import com.polysius.applocker.ui.SetupActions
import com.polysius.applocker.ui.SetupScreen

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    // Each launcher refreshes the checklist when the user returns from the system prompt.
    private val healthPermissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract(),
    ) { viewModel.refreshPermissions() }

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshPermissions() }

    private val setupActions = SetupActions(
        onGrant = ::requestGrant,
        onOpenAppInfo = { openSettings(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, withPackage = true) },
        onOpenHealthConnectSettings = ::openHealthConnectSettings,
        onOpenNotificationSettings = {
            val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            startActivity(intent)
        },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppLockerTheme { AppLockerRoot() }
        }
    }

    @Composable
    private fun AppLockerRoot() {
        val uiState by viewModel.uiState.collectAsStateWithLifecycle()

        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            // A rotation also stops the activity; only a real departure should re-prompt.
            if (!isChangingConfigurations) viewModel.onAppLeft()
        }

        // Permissions can change while the user is in Settings, so re-read them on every return.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

        Scaffold { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            when (uiState.screen) {
                Screen.LOADING -> Unit
                Screen.SETUP -> SetupScreen(
                    grants = uiState.grants,
                    anyPermissionNeedsAttention = uiState.anyPermissionNeedsAttention,
                    actions = setupActions,
                    onFinish = viewModel::finishSetup,
                    onNotNow = viewModel::showHome,
                    modifier = contentModifier,
                )

                Screen.HOME -> HomeScreen(
                    grants = uiState.grants,
                    onContinueSetup = viewModel::showSetup,
                    modifier = contentModifier,
                )
            }
        }
    }

    private fun requestGrant(kind: PermissionKind) {
        when (kind) {
            PermissionKind.ACCESSIBILITY -> openSettings(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            PermissionKind.USAGE_ACCESS -> openSettings(Settings.ACTION_USAGE_ACCESS_SETTINGS)
            PermissionKind.BATTERY ->
                openSettings(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, withPackage = true)
            PermissionKind.HEALTH_CONNECT ->
                healthPermissionLauncher.launch(viewModel.healthPermissions)
            PermissionKind.NOTIFICATIONS ->
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            PermissionKind.OVERLAY -> Unit // Skipped in Phase 0: the overlay permission is never requested.
        }
    }

    private fun openSettings(action: String, withPackage: Boolean = false) {
        val intent = Intent(action)
        if (withPackage) intent.data = Uri.fromParts("package", packageName, null)
        startActivity(intent)
    }

    private fun openHealthConnectSettings() {
        startActivity(Intent(HealthConnectClient.ACTION_HEALTH_CONNECT_SETTINGS))
    }
}

@Composable
private fun AppLockerTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val colorScheme = if (isSystemInDarkTheme()) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }
    MaterialTheme(colorScheme = colorScheme, content = content)
}
