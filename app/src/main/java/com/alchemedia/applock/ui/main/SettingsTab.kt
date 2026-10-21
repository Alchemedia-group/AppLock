package com.alchemedia.applock.ui.main

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alchemedia.applock.R
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.data.LockType
import com.alchemedia.applock.data.RelockMode
import com.alchemedia.applock.security.SetupStatus
import com.alchemedia.applock.security.isBiometricAvailable

@Composable
fun SettingsTab(
    settings: AppSettings,
    status: SetupStatus,
    onChangeCredential: () -> Unit,
    onBiometric: (Boolean) -> Unit,
    onShuffle: (Boolean) -> Unit,
    onCapture: (Boolean) -> Unit,
    onAttempts: (Int) -> Unit,
    onRelock: (RelockMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val biometricAvailable = isBiometricAvailable(context)
    val openAccessibility = openAccessibilitySettings()

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        MissingPermissionCards(status, needsCamera = settings.captureIntruders)

        SectionTitle(R.string.settings_section_status)
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text(
                stringResource(if (status.accessibilityEnabled) R.string.service_enabled else R.string.service_disabled),
                modifier = Modifier.weight(1f),
            )
            if (!status.accessibilityEnabled) {
                OutlinedButton(onClick = openAccessibility) { Text(stringResource(R.string.permission_accessibility_action)) }
            }
        }
        HorizontalDivider()

        SectionTitle(R.string.settings_section_lock)
        Button(onClick = onChangeCredential) { Text(stringResource(R.string.settings_change_credential)) }
        SwitchRow(
            title = stringResource(R.string.settings_biometric),
            subtitle = if (biometricAvailable) null else stringResource(R.string.settings_biometric_unavailable),
            checked = settings.biometricEnabled && biometricAvailable,
            enabled = biometricAvailable,
            onChange = onBiometric,
        )
        if (settings.lockType == LockType.PIN) {
            SwitchRow(
                title = stringResource(R.string.settings_shuffle),
                subtitle = stringResource(R.string.settings_shuffle_hint),
                checked = settings.shuffleKeypad,
                onChange = onShuffle,
            )
        }
        HorizontalDivider()

        SectionTitle(R.string.settings_section_intruder)
        SwitchRow(
            title = stringResource(R.string.settings_capture),
            checked = settings.captureIntruders,
            onChange = onCapture,
        )
        if (settings.captureIntruders) {
            Text(
                stringResource(R.string.settings_attempts, settings.attemptsBeforeCapture),
                style = MaterialTheme.typography.bodyMedium,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (1..5).forEach { n ->
                    FilterChip(
                        selected = settings.attemptsBeforeCapture == n,
                        onClick = { onAttempts(n) },
                        label = { Text(n.toString()) },
                    )
                }
            }
        }
        HorizontalDivider()

        SectionTitle(R.string.settings_section_relock)
        listOf(
            RelockMode.IMMEDIATE to R.string.relock_immediate,
            RelockMode.ONE_MINUTE to R.string.relock_one_minute,
            RelockMode.FIVE_MINUTES to R.string.relock_five_minutes,
            RelockMode.SCREEN_OFF to R.string.relock_screen_off,
        ).forEach { (mode, label) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(selected = settings.relockMode == mode, role = Role.RadioButton) { onRelock(mode) },
            ) {
                RadioButton(selected = settings.relockMode == mode, onClick = null)
                Text(stringResource(label), modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 8.dp))
            }
        }
        HorizontalDivider()

        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.settings_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(16.dp))
    }
}

@Composable
private fun SectionTitle(res: Int) {
    Text(
        stringResource(res),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun SwitchRow(
    title: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.weight(1f).padding(end = 16.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle != null) {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Switch(checked = checked, onCheckedChange = onChange, enabled = enabled)
    }
}
