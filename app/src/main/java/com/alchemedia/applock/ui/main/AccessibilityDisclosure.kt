package com.alchemedia.applock.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alchemedia.applock.R
import com.alchemedia.applock.ui.components.AppLogo

/**
 * Prominent disclosure shown before AppLock sends the user to Android's Accessibility settings.
 * It says what the service is used for and what it does not do, and asks for an explicit "Agree".
 */
@Composable
fun AccessibilityDisclosureScreen(onAgree: () -> Unit, onDecline: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        AppLogo(size = 64.dp)
        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.disclosure_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))
        Text(
            stringResource(R.string.disclosure_intro),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Start,
        )
        Spacer(Modifier.height(16.dp))
        Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceContainer, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Point(stringResource(R.string.disclosure_point_read))
                Point(stringResource(R.string.disclosure_point_share))
                Point(stringResource(R.string.disclosure_point_off))
            }
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onAgree, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(stringResource(R.string.disclosure_agree), style = MaterialTheme.typography.titleMedium)
        }
        TextButton(onClick = onDecline) { Text(stringResource(R.string.disclosure_decline)) }
    }
}

@Composable
private fun Point(text: String) {
    Row(verticalAlignment = Alignment.Top) {
        Text("✓", color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}
