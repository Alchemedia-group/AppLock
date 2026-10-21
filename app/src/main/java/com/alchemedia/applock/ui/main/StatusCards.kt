package com.alchemedia.applock.ui.main

import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.alchemedia.applock.R
import com.alchemedia.applock.security.SetupStatus

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

@Composable
fun rememberCameraRequest(): () -> Unit {
    // The status re-reads on resume, which fires when the permission dialog closes.
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    return { launcher.launch(android.Manifest.permission.CAMERA) }
}

@Composable
fun PermissionCard(
    title: String,
    body: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Text(body, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onErrorContainer)
            Spacer(Modifier.height(8.dp))
            Button(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** The accessibility and camera prompts, shown only for whatever is still missing. */
@Composable
fun MissingPermissionCards(
    status: SetupStatus,
    needsCamera: Boolean,
    modifier: Modifier = Modifier,
) {
    val openSettings = openAccessibilitySettings()
    val requestCamera = rememberCameraRequest()
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (!status.accessibilityEnabled) {
            PermissionCard(
                title = stringResource(R.string.permission_accessibility_title),
                body = stringResource(R.string.permission_accessibility_body),
                actionLabel = stringResource(R.string.permission_accessibility_action),
                onAction = openSettings,
            )
        }
        if (needsCamera && !status.cameraGranted) {
            PermissionCard(
                title = stringResource(R.string.permission_camera_title),
                body = stringResource(R.string.permission_camera_body),
                actionLabel = stringResource(R.string.permission_camera_action),
                onAction = requestCamera,
            )
        }
    }
}
