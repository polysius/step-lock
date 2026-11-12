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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polysius.applocker.R
import com.polysius.applocker.permissions.GrantState
import com.polysius.applocker.permissions.PermissionKind
import com.polysius.applocker.permissions.needsAttention

/** Phase 0 home: protection status and the permission checklist. Everything else comes later. */
@Composable
fun HomeScreen(
    grants: Map<PermissionKind, GrantState>,
    onContinueSetup: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)

        ProtectionStatusCard(accessibilityState = grants[PermissionKind.ACCESSIBILITY])

        Text(stringResource(R.string.home_phase_note))

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

/**
 * Protection is always OFF in Phase 0 because nothing enforces blocking yet. The text still says why,
 * so the app never suggests it is protecting anything (principle P3).
 */
@Composable
private fun ProtectionStatusCard(accessibilityState: GrantState?) {
    val message = if (accessibilityState == GrantState.GRANTED) {
        R.string.protection_off_not_built
    } else {
        R.string.protection_off_accessibility
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = stringResource(message),
            modifier = Modifier.padding(16.dp),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error,
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
