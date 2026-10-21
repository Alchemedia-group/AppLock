package com.alchemedia.applock.ui.main

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.alchemedia.applock.R

/**
 * Walks the user through the two awkward Android screens needed to enable protection, with a button
 * that opens each one. Step 2 ticks itself off once the accessibility service is running.
 */
@Composable
fun SetupGuideScreen(onClose: () -> Unit, modifier: Modifier = Modifier) {
    val status by rememberSetupStatus()
    val context = LocalContext.current
    val samsung = remember { Build.MANUFACTURER.equals("samsung", ignoreCase = true) }
    val openAccessibility = openAccessibilitySettings()
    val openAppInfo = {
        context.startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)),
        )
    }
    val enabled = status.accessibilityEnabled

    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                stringResource(R.string.guide_title),
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 8.dp),
            )
            TextButton(onClick = onClose) { Text(stringResource(R.string.guide_close)) }
        }
        Text(
            stringResource(R.string.guide_intro),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 8.dp),
        )

        GuideStep(number = 1, title = stringResource(R.string.guide_step1_title), done = false) {
            GuideText(stringResource(R.string.guide_step1_body), muted = true)
            MenuHint()
            GuideLine("1", stringResource(R.string.guide_step1_a))
            GuideLine("2", stringResource(R.string.guide_step1_b))
            GuideLine("3", stringResource(R.string.guide_step1_c))
            GuideText(stringResource(R.string.guide_step1_missing), muted = true)
            Button(onClick = openAppInfo) { Text(stringResource(R.string.guide_step1_action)) }
        }

        GuideStep(number = 2, title = stringResource(R.string.guide_step2_title), done = enabled) {
            GuideLine("1", stringResource(R.string.guide_step2_a))
            GuideLine(
                "2",
                stringResource(
                    R.string.guide_step2_b,
                    stringResource(if (samsung) R.string.guide_path_samsung else R.string.guide_path_other),
                ),
            )
            GuideLine("3", stringResource(R.string.guide_step2_c))
            Button(onClick = openAccessibility) { Text(stringResource(R.string.guide_step2_action)) }
        }

        GuideStep(number = 3, title = stringResource(R.string.guide_step3_title), done = enabled) {
            GuideText(
                stringResource(if (enabled) R.string.guide_step3_done else R.string.guide_step3_wait),
                muted = !enabled,
            )
            if (enabled) {
                Button(onClick = onClose) { Text(stringResource(R.string.guide_done)) }
            }
        }

        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.guide_trouble_title), style = MaterialTheme.typography.titleMedium)
                GuideText(stringResource(R.string.guide_trouble_grey), muted = true)
                GuideText(stringResource(R.string.guide_trouble_missing), muted = true)
                GuideText(stringResource(R.string.guide_trouble_off), muted = true)
                GuideText(stringResource(R.string.guide_trouble_brand), muted = true)
                FilledTonalButton(onClick = openAppInfo) { Text(stringResource(R.string.guide_step1_action)) }
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun GuideStep(
    number: Int,
    title: String,
    done: Boolean,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(if (done) colors.primary else colors.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        if (done) "✓" else number.toString(),
                        color = if (done) colors.onPrimary else colors.onPrimaryContainer,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Text(title, style = MaterialTheme.typography.titleMedium)
            }
            content()
        }
    }
}

@Composable
private fun GuideText(text: String, muted: Boolean = false) {
    Text(
        text,
        style = MaterialTheme.typography.bodyMedium,
        color = if (muted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun GuideLine(marker: String, text: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            marker,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.width(14.dp),
        )
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
}

/** Decorative mock of the App info top bar and menu, with the ⋮ button pulsing to show where to tap. */
@Composable
private fun MenuHint() {
    val colors = MaterialTheme.colorScheme
    val pulse = rememberInfiniteTransition(label = "menuPulse")
    val ring by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "ring",
    )
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colors.surfaceContainerHigh)
            .padding(14.dp)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("‹", style = MaterialTheme.typography.headlineSmall, color = colors.onSurfaceVariant)
            Spacer(Modifier.width(10.dp))
            Text(stringResource(R.string.guide_mock_title), style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Box(Modifier.size(36.dp), contentAlignment = Alignment.Center) {
                Box(
                    Modifier
                        .size(36.dp)
                        .scale(ring)
                        .alpha(0.35f)
                        .background(colors.primary, CircleShape),
                )
                Text("⋮", style = MaterialTheme.typography.titleLarge, color = colors.onSurface)
            }
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            Text(
                stringResource(R.string.guide_mock_item),
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Start,
                modifier = Modifier
                    .border(2.dp, colors.primary, RoundedCornerShape(12.dp))
                    .background(colors.surface, RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            )
        }
    }
}
