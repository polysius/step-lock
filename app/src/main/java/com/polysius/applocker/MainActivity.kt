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
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.polysius.applocker.permissions.PermissionKind
import com.polysius.applocker.ui.AppLockerTheme
import com.polysius.applocker.ui.AppsScreen
import com.polysius.applocker.ui.HistoryScreen
import com.polysius.applocker.ui.HomeScreen
import com.polysius.applocker.ui.MainViewModel
import com.polysius.applocker.ui.Screen
import com.polysius.applocker.ui.ScheduleScreen
import com.polysius.applocker.ui.SetupActions
import com.polysius.applocker.ui.SetupScreen

private enum class Tab(val labelRes: Int) {
    HOME(R.string.tab_home),
    APPS(R.string.tab_apps),
    SCHEDULE(R.string.tab_schedule),
    HISTORY(R.string.tab_history),
}

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
        val snackbarHostState = remember { SnackbarHostState() }
        var tab by rememberSaveable { mutableStateOf(Tab.HOME) }

        LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
            // A rotation also stops the activity; only a real departure should re-prompt.
            if (!isChangingConfigurations) viewModel.onAppLeft()
        }

        // Permissions can change while the user is in Settings, so re-read them on every return.
        LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { viewModel.refreshPermissions() }

        val context = this
        LaunchedEffect(Unit) {
            viewModel.messages.collect { message ->
                val text = if (message.argument != null) {
                    context.getString(message.res, message.argument)
                } else {
                    context.getString(message.res)
                }
                snackbarHostState.showSnackbar(text)
            }
        }

        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                if (uiState.screen == Screen.MAIN) {
                    NavigationBar {
                        Tab.entries.forEach { entry ->
                            NavigationBarItem(
                                selected = tab == entry,
                                onClick = { tab = entry },
                                icon = {},
                                label = { Text(stringResource(entry.labelRes)) },
                                alwaysShowLabel = true,
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            val contentModifier = Modifier.padding(innerPadding)
            when (uiState.screen) {
                Screen.LOADING -> Unit
                Screen.SETUP -> SetupScreen(
                    grants = uiState.grants,
                    anyPermissionNeedsAttention = uiState.anyPermissionNeedsAttention,
                    actions = setupActions,
                    onFinish = viewModel::finishSetup,
                    onNotNow = viewModel::showMain,
                    modifier = contentModifier,
                )

                Screen.MAIN -> MainTabs(tab, uiState.grants, contentModifier)
            }
        }
    }

    @Composable
    private fun MainTabs(
        tab: Tab,
        grants: Map<PermissionKind, com.polysius.applocker.permissions.GrantState>,
        modifier: Modifier,
    ) {
        val enforcementState by viewModel.enforcementState.collectAsStateWithLifecycle()
        val settings by viewModel.settings.collectAsStateWithLifecycle()
        val rules by viewModel.rules.collectAsStateWithLifecycle()
        val savedApps by viewModel.lockedApps.collectAsStateWithLifecycle()
        val installedApps by viewModel.installedApps.collectAsStateWithLifecycle()
        val history by viewModel.history.collectAsStateWithLifecycle()

        when (tab) {
            Tab.HOME -> HomeScreen(
                grants = grants,
                enforcementState = enforcementState,
                onSetProtection = viewModel::setProtectionEnabled,
                onContinueSetup = viewModel::showSetup,
                modifier = modifier,
            )

            Tab.APPS -> AppsScreen(
                installedApps = installedApps,
                savedApps = savedApps,
                allAppsMode = settings?.allAppsMode == true,
                isAlwaysAllowed = viewModel::isAlwaysAllowed,
                onSetAllAppsMode = viewModel::setAllAppsMode,
                onSetLocked = viewModel::setLocked,
                onSetAllowlisted = viewModel::setAllowlisted,
                modifier = modifier,
            )

            Tab.SCHEDULE -> ScheduleScreen(
                rules = rules,
                settings = settings,
                onSaveRule = viewModel::saveRule,
                onDeleteRule = viewModel::deleteRule,
                onSetBedtime = viewModel::setBedtime,
                modifier = modifier,
            )

            Tab.HISTORY -> HistoryScreen(entries = history, modifier = modifier)
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
            PermissionKind.OVERLAY -> Unit // Skipped: the lock screen is an Accessibility overlay, which needs no permission.
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
