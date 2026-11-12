package com.polysius.applocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polysius.applocker.R
import com.polysius.applocker.data.LockedAppEntity

/**
 * Picks which apps are locked (list mode) or which stay open (all-apps mode). Sorted with the
 * selected apps first, then A to Z, and the screen says so.
 */
@Composable
fun AppsScreen(
    installedApps: List<InstalledApp>,
    savedApps: List<LockedAppEntity>,
    allAppsMode: Boolean,
    isAlwaysAllowed: (String) -> Boolean,
    onSetAllAppsMode: (Boolean) -> Unit,
    onSetLocked: (InstalledApp, Boolean) -> Unit,
    onSetAllowlisted: (InstalledApp, Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var query by rememberSaveable { mutableStateOf("") }
    val savedByPackage = remember(savedApps) { savedApps.associateBy { it.packageName } }

    fun isSelected(app: InstalledApp): Boolean {
        val saved = savedByPackage[app.packageName]
        return if (allAppsMode) saved?.isAllowlisted == true else saved?.isLocked == true
    }

    val visibleApps = remember(installedApps, savedByPackage, allAppsMode, query) {
        installedApps
            .filter { it.label.contains(query, ignoreCase = true) }
            .sortedWith(compareByDescending<InstalledApp> { isSelected(it) }.thenBy { it.label.lowercase() })
    }

    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.apps_title), style = MaterialTheme.typography.headlineMedium)

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(stringResource(R.string.apps_all_apps_mode), style = MaterialTheme.typography.titleMedium)
                Text(
                    stringResource(
                        if (allAppsMode) R.string.apps_all_apps_on else R.string.apps_all_apps_off,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            Switch(checked = allAppsMode, onCheckedChange = onSetAllAppsMode)
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            label = { Text(stringResource(R.string.apps_search)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(stringResource(R.string.apps_sort_note), style = MaterialTheme.typography.bodySmall)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            items(visibleApps, key = { it.packageName }) { app ->
                AppRow(
                    app = app,
                    selected = isSelected(app),
                    alwaysAllowed = isAlwaysAllowed(app.packageName),
                    allAppsMode = allAppsMode,
                    onToggle = { selected ->
                        if (allAppsMode) onSetAllowlisted(app, selected) else onSetLocked(app, selected)
                    },
                )
            }
        }
    }
}

@Composable
private fun AppRow(
    app: InstalledApp,
    selected: Boolean,
    alwaysAllowed: Boolean,
    allAppsMode: Boolean,
    onToggle: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AppIcon(app.packageName)
        Column(modifier = Modifier.weight(1f)) {
            Text(app.label)
            if (alwaysAllowed) {
                Text(stringResource(R.string.apps_always_allowed), style = MaterialTheme.typography.bodySmall)
            } else {
                val captionRes = when {
                    allAppsMode && selected -> R.string.apps_caption_allowed
                    allAppsMode -> R.string.apps_caption_locked
                    selected -> R.string.apps_caption_locked
                    else -> R.string.apps_caption_open
                }
                Text(stringResource(captionRes), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (!alwaysAllowed) {
            Switch(checked = selected, onCheckedChange = onToggle)
        }
    }
}
