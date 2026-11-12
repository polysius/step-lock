package com.polysius.applocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polysius.applocker.R
import com.polysius.applocker.blocking.EnforcementState
import com.polysius.applocker.permissions.GrantState
import com.polysius.applocker.permissions.PermissionKind
import com.polysius.applocker.permissions.needsAttention
import com.polysius.applocker.rules.Decision
import com.polysius.applocker.rules.LockReason
import com.polysius.applocker.rules.RulesEngine

private enum class ProtectionStatus { ON, DEGRADED, OFF }

/** Protection status, the current lock state, and the permission checklist. */
@Composable
fun HomeScreen(
    grants: Map<PermissionKind, GrantState>,
    enforcementState: EnforcementState?,
    onSetProtection: (Boolean) -> Unit,
    onContinueSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val now by rememberNow()
    val accessibilityGranted = grants[PermissionKind.ACCESSIBILITY] == GrantState.GRANTED
    val enabled = enforcementState?.protectionEnabled == true
    val status = when {
        enabled && accessibilityGranted -> ProtectionStatus.ON
        enabled -> ProtectionStatus.DEGRADED
        else -> ProtectionStatus.OFF
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)

        ProtectionCard(
            status = status,
            canTurnOn = accessibilityGranted,
            onSetProtection = onSetProtection,
        )

        if (enforcementState != null) {
            Text(lockSummary(enforcementState, now))
        }

        Text(
            stringResource(R.string.home_checklist_title),
            style = MaterialTheme.typography.titleMedium,
        )
        PermissionKind.entries.forEach { kind ->
            ChecklistRow(kind = kind, state = grants[kind] ?: GrantState.MISSING)
        }

        if (grants.values.any { it.needsAttention }) {
            Button(onClick = onContinueSetup) {
                Text(stringResource(R.string.home_continue_setup))
            }
        }
    }
}

@Composable
private fun lockSummary(state: EnforcementState, now: java.time.LocalDateTime): String =
    when (val lock = RulesEngine.currentLock(now, state.config)) {
        Decision.Allow -> stringResource(R.string.home_open_now)
        is Decision.Lock -> {
            val until = timeFormat.format(lock.endsAt)
            if (lock.reason == LockReason.BEDTIME) {
                stringResource(R.string.home_locked_bedtime, until)
            } else {
                stringResource(R.string.home_locked_block, lock.ruleName.orEmpty(), until)
            }
        }
    }

@Composable
private fun ProtectionCard(
    status: ProtectionStatus,
    canTurnOn: Boolean,
    onSetProtection: (Boolean) -> Unit,
) {
    var confirmingOff by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            val (titleRes, color) = when (status) {
                ProtectionStatus.ON -> R.string.protection_on to MaterialTheme.colorScheme.primary
                ProtectionStatus.DEGRADED -> R.string.protection_degraded to MaterialTheme.colorScheme.error
                ProtectionStatus.OFF -> R.string.protection_off to MaterialTheme.colorScheme.error
            }
            Text(stringResource(titleRes), style = MaterialTheme.typography.titleMedium, color = color)

            if (status == ProtectionStatus.OFF) {
                Button(onClick = { onSetProtection(true) }, enabled = canTurnOn) {
                    Text(stringResource(R.string.protection_turn_on))
                }
                if (!canTurnOn) Text(stringResource(R.string.protection_needs_accessibility))
            } else {
                OutlinedButton(onClick = { confirmingOff = true }) {
                    Text(stringResource(R.string.protection_turn_off))
                }
            }
        }
    }

    if (confirmingOff) {
        AlertDialog(
            onDismissRequest = { confirmingOff = false },
            title = { Text(stringResource(R.string.protection_turn_off_title)) },
            text = { Text(stringResource(R.string.protection_turn_off_body)) },
            confirmButton = {
                TextButton(onClick = {
                    confirmingOff = false
                    onSetProtection(false)
                }) { Text(stringResource(R.string.protection_turn_off)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmingOff = false }) {
                    Text(stringResource(R.string.dialog_cancel))
                }
            },
        )
    }
}

@Composable
private fun ChecklistRow(kind: PermissionKind, state: GrantState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(stringResource(kind.titleRes))
        Text(
            stringResource(state.labelRes),
            color = if (state.needsAttention) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.primary
            },
        )
    }
}
