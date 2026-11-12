package com.polysius.applocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polysius.applocker.R
import com.polysius.applocker.permissions.GrantState
import com.polysius.applocker.permissions.PermissionKind
import com.polysius.applocker.permissions.needsAttention

/**
 * First-launch flow (FR-48): one permission per step, each with a plain-language reason and a
 * button that opens the exact system screen for that grant.
 */
@Composable
fun SetupScreen(
    grants: Map<PermissionKind, GrantState>,
    anyPermissionNeedsAttention: Boolean,
    actions: SetupActions,
    onFinish: () -> Unit,
    onNotNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val steps = PermissionKind.entries
    var stepIndex by rememberSaveable { mutableIntStateOf(0) }
    val kind = steps[stepIndex]
    val state = grants[kind] ?: GrantState.MISSING
    val isLastStep = stepIndex == steps.lastIndex

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.setup_title), style = MaterialTheme.typography.headlineMedium)
        Text(
            stringResource(R.string.setup_step_counter, stepIndex + 1, steps.size),
            style = MaterialTheme.typography.labelLarge,
        )

        PermissionStepCard(kind = kind, state = state, actions = actions)

        if (anyPermissionNeedsAttention) {
            Text(
                stringResource(R.string.setup_warning_protection_off),
                color = MaterialTheme.colorScheme.error,
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            OutlinedButton(onClick = { stepIndex -= 1 }, enabled = stepIndex > 0) {
                Text(stringResource(R.string.setup_back))
            }
            if (isLastStep) {
                Button(onClick = onFinish) { Text(stringResource(R.string.setup_finish)) }
            } else {
                Button(onClick = { stepIndex += 1 }) { Text(stringResource(R.string.setup_next)) }
            }
        }

        TextButton(onClick = onNotNow) { Text(stringResource(R.string.setup_not_now)) }
    }
}

@Composable
private fun PermissionStepCard(
    kind: PermissionKind,
    state: GrantState,
    actions: SetupActions,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(kind.titleRes), style = MaterialTheme.typography.titleLarge)
            Text(
                stringResource(state.labelRes),
                style = MaterialTheme.typography.labelLarge,
                color = if (state.needsAttention) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.primary
                },
            )
            Text(stringResource(kind.whyRes))

            if (state.needsAttention) {
                GrantButtons(kind = kind, state = state, actions = actions)
            }
        }
    }
}

@Composable
private fun GrantButtons(kind: PermissionKind, state: GrantState, actions: SetupActions) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        when (kind) {
            PermissionKind.ACCESSIBILITY,
            PermissionKind.USAGE_ACCESS,
            PermissionKind.BATTERY -> {
                Button(onClick = { actions.onGrant(kind) }) {
                    Text(stringResource(R.string.setup_open_settings))
                }
            }

            PermissionKind.HEALTH_CONNECT -> {
                if (state == GrantState.UNAVAILABLE) {
                    Text(stringResource(R.string.health_connect_unavailable))
                } else {
                    Button(onClick = { actions.onGrant(kind) }) {
                        Text(stringResource(R.string.setup_allow))
                    }
                    Text(stringResource(R.string.health_connect_denied_hint))
                    OutlinedButton(onClick = actions.onOpenHealthConnectSettings) {
                        Text(stringResource(R.string.health_connect_open_settings))
                    }
                }
            }

            PermissionKind.NOTIFICATIONS -> {
                Button(onClick = { actions.onGrant(kind) }) {
                    Text(stringResource(R.string.setup_allow))
                }
                OutlinedButton(onClick = actions.onOpenNotificationSettings) {
                    Text(stringResource(R.string.notifications_open_settings))
                }
            }

            PermissionKind.OVERLAY -> Unit // Skipped: nothing to grant.
        }

        if (kind.mayBeRestricted) {
            RestrictedSettingsHelp(onOpenAppInfo = actions.onOpenAppInfo)
        }
    }
}

@Composable
private fun RestrictedSettingsHelp(onOpenAppInfo: () -> Unit) {
    Card {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                stringResource(R.string.restricted_settings_title),
                style = MaterialTheme.typography.titleSmall,
            )
            Text(stringResource(R.string.restricted_settings_body))
            OutlinedButton(onClick = onOpenAppInfo) {
                Text(stringResource(R.string.setup_open_app_info))
            }
        }
    }
}
