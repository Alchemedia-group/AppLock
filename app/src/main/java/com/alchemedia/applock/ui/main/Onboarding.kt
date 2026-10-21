package com.alchemedia.applock.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.PersistableBundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.alchemedia.applock.R
import com.alchemedia.applock.security.RecoveryCode
import com.alchemedia.applock.security.SetupStatus
import com.alchemedia.applock.ui.components.GroupCard
import com.alchemedia.applock.ui.components.IconTile
import com.alchemedia.applock.ui.components.SettingRow

private const val STEP_COUNT = 4

/** First-run flow: welcome, choose PIN/pattern, save recovery code, grant permissions. */
@Composable
fun OnboardingFlow(viewModel: MainViewModel) {
    var step by rememberSaveable { mutableIntStateOf(0) }
    var recoveryCode by rememberSaveable { mutableStateOf(RecoveryCode.generate()) }

    Box(Modifier.fillMaxSize()) {
        when (step) {
            0 -> WelcomeStep(onStart = { step = 1 })
            1 -> CredentialSetup(
                onDone = { type, secret ->
                    viewModel.setCredential(type, secret, authenticate = false)
                    step = 2
                },
                onCancel = { step = 0 },
            )
            2 -> RecoveryCodeScreen(
                code = recoveryCode,
                onSaved = {
                    viewModel.saveRecoveryCode(recoveryCode)
                    step = 3
                },
            )
            else -> PermissionsStep(
                onEnableNotifications = { viewModel.setHideNotifications(true) },
                onFinish = { viewModel.completeSetup() },
            )
        }
        StepDots(
            current = step,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 10.dp),
        )
    }
}

@Composable
private fun StepDots(current: Int, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        repeat(STEP_COUNT) { index ->
            Box(
                Modifier
                    .height(5.dp)
                    .width(if (index == current) 26.dp else 10.dp)
                    .clip(CircleShape)
                    .background(
                        if (index <= current) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                    ),
            )
        }
    }
}

@Composable
private fun WelcomeStep(onStart: () -> Unit) {
    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(painterResource(R.drawable.ic_shield), size = 84.dp)
        Spacer(Modifier.height(24.dp))
        Text(
            stringResource(R.string.onboard_welcome_title),
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.onboard_welcome_body),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.height(28.dp))
        GroupCard {
            SettingRow(
                title = stringResource(R.string.onboard_feature_lock_title),
                subtitle = stringResource(R.string.onboard_feature_lock_body),
                icon = rememberVectorPainter(Icons.Default.Lock),
            )
            SettingRow(
                title = stringResource(R.string.onboard_feature_photo_title),
                subtitle = stringResource(R.string.onboard_feature_photo_body),
                icon = painterResource(R.drawable.ic_camera),
            )
            SettingRow(
                title = stringResource(R.string.onboard_feature_private_title),
                subtitle = stringResource(R.string.onboard_feature_private_body),
                icon = painterResource(R.drawable.ic_shield),
            )
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = onStart, modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(54.dp)) {
            Text(stringResource(R.string.onboard_get_started), style = MaterialTheme.typography.titleMedium)
        }
    }
}

/** Shows a freshly generated recovery code once. Used at setup and when the user makes a new one. */
@Composable
fun RecoveryCodeScreen(
    code: String,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier,
    onCancel: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(painterResource(R.drawable.ic_shield), size = 64.dp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.recovery_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.recovery_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        Surface(shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surfaceContainerHigh) {
            Text(
                code,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.SemiBold,
                fontSize = 26.sp,
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp, horizontal = 12.dp),
            )
        }
        Spacer(Modifier.height(12.dp))
        FilledTonalButton(onClick = {
            copyToClipboard(context, code)
            copied = true
        }) {
            Text(stringResource(if (copied) R.string.recovery_copied else R.string.recovery_copy))
        }
        if (onCancel != null) {
            Text(
                stringResource(R.string.recovery_new_code_notice),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 12.dp),
            )
        }
        Spacer(Modifier.height(24.dp))
        Button(onClick = onSaved, modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Text(stringResource(R.string.recovery_saved), style = MaterialTheme.typography.titleMedium)
        }
        if (onCancel != null) {
            TextButton(onClick = onCancel) { Text(stringResource(R.string.cancel)) }
        }
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clip = ClipData.newPlainText("AppLock recovery code", text)
    // Ask Android 13+ to mask the clip preview.
    clip.description.extras = PersistableBundle().apply { putBoolean("android.content.extra.IS_SENSITIVE", true) }
    context.getSystemService(ClipboardManager::class.java).setPrimaryClip(clip)
}

@Composable
private fun PermissionsStep(onEnableNotifications: () -> Unit, onFinish: () -> Unit) {
    val status by rememberSetupStatus()
    val openAccessibility = openAccessibilitySettings()
    val requestCamera = rememberCameraRequest()
    val requestNotifications = rememberNotificationAccessRequest(status.notificationListenerEnabled)

    Column(
        Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            stringResource(R.string.onboard_permissions_title),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.onboard_permissions_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp),
        )
        Spacer(Modifier.height(24.dp))
        GroupCard {
            PermissionRow(
                title = stringResource(R.string.perm_service_title),
                body = stringResource(R.string.perm_service_body),
                icon = painterResource(R.drawable.ic_shield),
                granted = status.accessibilityEnabled,
                onEnable = openAccessibility,
            )
            PermissionRow(
                title = stringResource(R.string.perm_camera_title),
                body = stringResource(R.string.perm_camera_body),
                icon = painterResource(R.drawable.ic_camera),
                granted = status.cameraGranted,
                onEnable = requestCamera,
            )
            PermissionRow(
                title = stringResource(R.string.perm_notifications_title),
                body = stringResource(R.string.perm_notifications_body),
                icon = rememberVectorPainter(Icons.Default.Lock),
                granted = status.notificationListenerEnabled && status.postNotificationsGranted,
                onEnable = {
                    onEnableNotifications()
                    requestNotifications()
                },
            )
        }
        if (!status.accessibilityEnabled) {
            Text(
                stringResource(R.string.perm_restricted_hint),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 32.dp, vertical = 12.dp),
            )
        }
        Spacer(Modifier.height(20.dp))
        if (status.accessibilityEnabled) {
            Button(onClick = onFinish, modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(54.dp)) {
                Text(stringResource(R.string.onboard_finish), style = MaterialTheme.typography.titleMedium)
            }
        } else {
            OutlinedButton(onClick = onFinish, modifier = Modifier.padding(horizontal = 24.dp).fillMaxWidth().height(54.dp)) {
                Text(stringResource(R.string.onboard_skip), style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    body: String,
    icon: androidx.compose.ui.graphics.painter.Painter,
    granted: Boolean,
    onEnable: () -> Unit,
) {
    SettingRow(
        title = title,
        subtitle = body,
        icon = icon,
        trailing = {
            if (granted) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Text(
                        stringResource(R.string.perm_granted),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                    )
                }
            } else {
                FilledTonalButton(onClick = onEnable) { Text(stringResource(R.string.perm_enable)) }
            }
        },
    )
}

/** Asks for the recovery code after "Forgot PIN or pattern?". */
@Composable
fun RecoverySignIn(
    onVerify: (code: String, onResult: (Boolean) -> Unit) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var code by rememberSaveable { mutableStateOf("") }
    var wrong by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        IconTile(painterResource(R.drawable.ic_shield), size = 64.dp)
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.recovery_enter_title), style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            stringResource(R.string.recovery_enter_body),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = code,
            onValueChange = {
                code = it.uppercase()
                wrong = false
            },
            label = { Text(stringResource(R.string.recovery_code_label)) },
            singleLine = true,
            isError = wrong,
            supportingText = { if (wrong) Text(stringResource(R.string.recovery_wrong)) },
            textStyle = MaterialTheme.typography.titleMedium.copy(fontFamily = FontFamily.Monospace, letterSpacing = 1.sp),
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, imeAction = ImeAction.Done),
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = { onVerify(code) { ok -> if (!ok) wrong = true } },
            enabled = code.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp),
        ) { Text(stringResource(R.string.recovery_verify), style = MaterialTheme.typography.titleMedium) }
        TextButton(onClick = onBack) { Text(stringResource(R.string.onboard_back)) }
    }
}

