package com.polysius.applocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.polysius.applocker.R
import com.polysius.applocker.data.HistoryEntryEntity
import java.time.Instant
import java.time.ZoneId

/** Local history of block attempts and changes, newest first. */
@Composable
fun HistoryScreen(entries: List<HistoryEntryEntity>, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(stringResource(R.string.history_title), style = MaterialTheme.typography.headlineMedium)
        if (entries.isEmpty()) {
            Text(stringResource(R.string.history_empty))
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(entries, key = { it.id }) { entry ->
                Column {
                    Text(entry.detail)
                    val time = Instant.ofEpochMilli(entry.timestampMillis)
                        .atZone(ZoneId.systemDefault())
                        .format(dateTimeFormat)
                    Text(
                        "$time · ${entry.type.name.lowercase().replace('_', ' ')}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}
