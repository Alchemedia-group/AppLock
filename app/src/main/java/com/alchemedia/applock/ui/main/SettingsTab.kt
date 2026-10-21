package com.alchemedia.applock.ui.main

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.alchemedia.applock.R
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.data.LockType
import com.alchemedia.applock.data.RelockMode
import com.alchemedia.applock.security.SetupStatus
import com.alchemedia.applock.security.isBiometricAvailable
import com.alchemedia.applock.ui.components.GroupCard
import com.alchemedia.applock.ui.components.SectionLabel
import com.alchemedia.applock.ui.components.Segmented
import com.alchemedia.applock.ui.components.SettingRow

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
    val colors = MaterialTheme.colorScheme

    Column(
        modifier
            .verticalScroll(rememberScrollState())
            .padding(bottom = 112.dp),
    ) {
        SectionLabel(stringResource(R.string.settings_section_status))
        GroupCard {
            SettingRow(
                title = stringResource(if (status.accessibilityEnabled) R.string.service_enabled else R.string.service_disabled),
                icon = painterResource(R.drawable.ic_shield),
                trailing = {
                    if (!status.accessibilityEnabled) {
                        OutlinedButton(onClick = openAccessibility) { Text(stringResource(R.string.permission_accessibility_action)) }
                    }
                },
            )
        }

        SectionLabel(stringResource(R.string.settings_section_lock))
        GroupCard {
            SettingRow(
                title = stringResource(R.string.settings_change_credential),
                icon = rememberVectorPainter(Icons.Default.Lock),
                onClick = onChangeCredential,
                trailing = { Text(stringResource(R.string.chevron), style = MaterialTheme.typography.titleLarge, color = colors.onSurfaceVariant) },
            )
            SettingRow(
                title = stringResource(R.string.settings_biometric),
                subtitle = if (biometricAvailable) null else stringResource(R.string.settings_biometric_unavailable),
                trailing = {
                    Switch(
                        checked = settings.biometricEnabled && biometricAvailable,
                        onCheckedChange = onBiometric,
                        enabled = biometricAvailable,
                    )
                },
            )
            if (settings.lockType == LockType.PIN) {
                SettingRow(
                    title = stringResource(R.string.settings_shuffle),
                    subtitle = stringResource(R.string.settings_shuffle_hint),
                    trailing = { Switch(checked = settings.shuffleKeypad, onCheckedChange = onShuffle) },
                )
            }
        }

        SectionLabel(stringResource(R.string.settings_section_intruder))
        GroupCard {
            SettingRow(
                title = stringResource(R.string.settings_capture),
                icon = painterResource(R.drawable.ic_camera),
                trailing = { Switch(checked = settings.captureIntruders, onCheckedChange = onCapture) },
            )
            if (settings.captureIntruders) {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        stringResource(R.string.settings_attempts, settings.attemptsBeforeCapture),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.onSurfaceVariant,
                    )
                    Segmented(
                        options = (1..5).map { it.toString() },
                        selected = settings.attemptsBeforeCapture - 1,
                        onSelect = { onAttempts(it + 1) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        SectionLabel(stringResource(R.string.settings_section_relock))
        GroupCard {
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
                        .clickable(role = Role.RadioButton) { onRelock(mode) }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    RadioButton(selected = settings.relockMode == mode, onClick = null, modifier = Modifier.padding(12.dp))
                    Text(stringResource(label), style = MaterialTheme.typography.bodyLarge)
                }
            }
        }

        Spacer(Modifier.height(16.dp))
        Text(
            stringResource(R.string.settings_privacy),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 28.dp),
        )
    }
}
