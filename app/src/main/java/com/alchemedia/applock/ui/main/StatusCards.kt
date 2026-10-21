package com.alchemedia.applock.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.alchemedia.applock.R
import com.alchemedia.applock.security.SetupStatus
import com.alchemedia.applock.security.canPostNotifications
import com.alchemedia.applock.ui.components.IconTile

/** Permission/service status that re-reads whenever the app returns to the foreground. */
@Composable
fun rememberSetupStatus(): State<SetupStatus> {
    val context = LocalContext.current
    val status = remember { mutableStateOf(SetupStatus.read(context)) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { status.value = SetupStatus.read(context) }
    return status
}

@Composable
fun openAccessibilitySettings(): () -> Unit {
    val context = LocalContext.current
    return { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)) }
}

/** Asks for POST_NOTIFICATIONS where needed, then sends the user to the notification-access screen. */
@Composable
fun rememberNotificationAccessRequest(listenerEnabled: Boolean): () -> Unit {
    val context = LocalContext.current
    val openListenerSettings = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        if (!listenerEnabled) openListenerSettings()
    }
    return {
        if (!canPostNotifications(context)) {
            launcher.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        } else if (!listenerEnabled) {
            openListenerSettings()
        }
    }
}

@Composable
fun rememberCameraRequest(): () -> Unit {
    // The status re-reads on resume, which fires when the permission dialog closes.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return { launcher.launch(android.Manifest.permission.CAMERA) }
}

/** Big status card at the top of the Apps tab: green-lit when the service runs, coral when it doesn't. */
@Composable
fun ProtectionHero(
    active: Boolean,
    lockedCount: Int,
    onTurnOn: () -> Unit,
    onHelp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val brush = if (active) {
        Brush.linearGradient(listOf(colors.primaryContainer, colors.surfaceContainerHigh))
    } else {
        Brush.linearGradient(listOf(colors.errorContainer, colors.surfaceContainerHigh))
    }
    val accent = if (active) colors.primary else colors.error
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.extraLarge,
        color = colors.surfaceContainer,
    ) {
        Column(
            Modifier
                .background(brush)
                .padding(20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(60.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(painterResource(R.drawable.ic_shield), contentDescription = null, tint = accent, modifier = Modifier.size(32.dp))
                }
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(if (active) R.string.hero_active else R.string.hero_off),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        when {
                            !active -> stringResource(R.string.hero_off_sub)
                            lockedCount == 0 -> stringResource(R.string.hero_none)
                            else -> pluralStringResource(R.plurals.hero_locked_count, lockedCount, lockedCount)
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                }
            }
            if (!active) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onHelp,
                    colors = ButtonDefaults.buttonColors(containerColor = colors.error, contentColor = colors.onError),
                ) { Text(stringResource(R.string.help_link)) }
                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onTurnOn) { Text(stringResource(R.string.permission_accessibility_action)) }
            }
        }
    }
}

/** Slim prompt for the camera permission, shown only while intruder photos are on and it is missing. */
@Composable
fun CameraPermissionBanner(modifier: Modifier = Modifier) {
    val request = rememberCameraRequest()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainer,
    ) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(
                painterResource(R.drawable.ic_camera),
                tint = MaterialTheme.colorScheme.error,
                background = MaterialTheme.colorScheme.errorContainer,
            )
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(stringResource(R.string.permission_camera_title), style = MaterialTheme.typography.titleSmall)
                Text(
                    stringResource(R.string.permission_camera_body),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(10.dp))
            Button(onClick = request) { Text(stringResource(R.string.permission_camera_action)) }
        }
    }
}
