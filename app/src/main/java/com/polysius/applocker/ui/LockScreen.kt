package com.polysius.applocker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.polysius.applocker.R

/** Plain text for the lock screen. It never contains anything from the blocked app (FR-07). */
data class LockScreenInfo(
    val packageName: String,
    val appLabel: String,
    val headline: String,
    val detail: String,
)

/**
 * The lock screen (FR-06). It has one action, "Back to Home". There is no unlock, emergency or
 * override button, and none may be added (FR-43).
 */
@Composable
fun LockScreen(info: LockScreenInfo, onBackToHome: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surface) {
        Column(
            modifier = Modifier.safeDrawingPadding().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon(info.packageName, size = 72.dp)
            Text(info.appLabel, style = MaterialTheme.typography.titleLarge)
            Text(
                info.headline,
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
            )
            Text(info.detail, textAlign = TextAlign.Center)
            Text(
                stringResource(R.string.lock_no_override),
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onBackToHome) { Text(stringResource(R.string.lock_back_to_home)) }
        }
    }
}
