package com.alchemedia.applock.ui.main

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.alchemedia.applock.R
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.ui.components.AppLogo
import com.alchemedia.applock.security.RecoveryCode

private enum class Tab(
    val label: Int,
    val title: Int,
    val subtitle: Int,
    val icon: @Composable () -> Painter,
) {
    Apps(R.string.tab_apps, R.string.header_apps, R.string.header_apps_sub, { rememberVectorPainter(Icons.Default.Lock) }),
    Intruders(R.string.tab_intruders, R.string.header_intruders, R.string.header_intruders_sub, { painterResource(R.drawable.ic_camera) }),
    Settings(R.string.tab_settings, R.string.header_settings, R.string.header_settings_sub, { rememberVectorPainter(Icons.Default.Settings) }),
}

@Composable
fun HomeScreen(settings: AppSettings, viewModel: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var changingCredential by rememberSaveable { mutableStateOf(false) }
    var newRecoveryCode by rememberSaveable { mutableStateOf<String?>(null) }
    var showGuide by rememberSaveable { mutableStateOf(false) }
    var showPro by rememberSaveable { mutableStateOf(false) }

    val apps by viewModel.apps.collectAsState()
    val shots by viewModel.shots.collectAsState()
    val statusState = rememberSetupStatus()
    val status by statusState

    val autoDeleteDays by rememberUpdatedState(settings.autoDeleteDays)
    val backupMessage by viewModel.backupMessage.collectAsState()

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadApps()
        viewModel.purgeOldShots(autoDeleteDays)
        viewModel.refreshShots()
    }
    LaunchedEffect(settings.autoDeleteDays) {
        viewModel.purgeOldShots(settings.autoDeleteDays)
        viewModel.refreshShots()
    }

    if (changingCredential) {
        CredentialSetup(
            onDone = { type, secret ->
                viewModel.setCredential(type, secret)
                changingCredential = false
            },
            onCancel = { changingCredential = false },
        )
        return
    }

    val billing by viewModel.billingState.collectAsState()
    val priceText = billing.priceText ?: stringResource(R.string.pro_price_fallback)

    if (showPro) {
        ProScreen(settings = settings, viewModel = viewModel, onClose = { showPro = false })
        return
    }

    if (showGuide) {
        SetupGuideScreen(onClose = { showGuide = false })
        return
    }

    newRecoveryCode?.let { code ->
        RecoveryCodeScreen(
            code = code,
            onSaved = {
                viewModel.saveRecoveryCode(code)
                newRecoveryCode = null
            },
            onCancel = { newRecoveryCode = null },
        )
        return
    }

    val current = Tab.entries[tab]
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding(),
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(start = 24.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(stringResource(current.title), style = MaterialTheme.typography.headlineMedium)
                    Text(
                        stringResource(current.subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                AppLogo(size = 40.dp)
            }
            val content = Modifier.weight(1f)
            when (current) {
                Tab.Apps -> AppsTab(
                    settings = settings,
                    apps = apps,
                    status = status,
                    onToggle = viewModel::setLocked,
                    onHelp = { showGuide = true },
                    onUpgrade = { showPro = true },
                    priceText = priceText,
                    modifier = content,
                )
                Tab.Intruders -> IntrudersTab(
                    shots = shots,
                    onDelete = viewModel::deleteShot,
                    onDeleteAll = viewModel::deleteAllShots,
                    isPro = settings.proUnlocked,
                    onUpgrade = { showPro = true },
                    modifier = content,
                )
                Tab.Settings -> SettingsTab(
                    settings = settings,
                    status = status,
                    onChangeCredential = { changingCredential = true },
                    onHelp = { showGuide = true },
                    onUpgrade = { showPro = true },
                    priceText = priceText,
                    onBiometric = { viewModel.setBiometricEnabled(it) },
                    onShuffle = { viewModel.setShuffleKeypad(it) },
                    onCapture = { viewModel.setCaptureIntruders(it) },
                    onAttempts = { viewModel.setAttemptsBeforeCapture(it) },
                    onRelock = { viewModel.setRelockMode(it) },
                    onFakeCrash = { viewModel.setFakeCrash(it) },
                    onHideNotifications = { viewModel.setHideNotifications(it) },
                    onAutoDelete = { viewModel.setAutoDeleteDays(it) },
                    onNewRecoveryCode = { newRecoveryCode = RecoveryCode.generate() },
                    onExport = { viewModel.exportBackup(it) },
                    onImport = { viewModel.importBackup(it) },
                    backupMessage = backupMessage,
                    modifier = content,
                )
            }
        }
        // Floats over the lists, which keep bottom padding so the last row can scroll clear of it.
        Box(Modifier.align(Alignment.BottomCenter)) {
            FloatingNav(selected = tab, onSelect = {
                tab = it
                viewModel.clearBackupMessage()
            })
        }
    }
}

/** Floating pill navigation: the selected tab expands to show its label. */
@Composable
private fun FloatingNav(selected: Int, onSelect: (Int) -> Unit) {
    val colors = MaterialTheme.colorScheme
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 32.dp, end = 32.dp, bottom = 20.dp),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            shape = CircleShape,
            color = colors.surfaceContainerHigh,
            shadowElevation = 14.dp,
        ) {
            Row(Modifier.padding(6.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Tab.entries.forEachIndexed { index, entry ->
                    val on = index == selected
                    val background by animateColorAsState(if (on) colors.primary else Color.Transparent, label = "tabBg")
                    val tint = if (on) colors.onPrimary else colors.onSurfaceVariant
                    Row(
                        Modifier
                            .clip(CircleShape)
                            .background(background)
                            .clickable(role = Role.Tab) { onSelect(index) }
                            .padding(horizontal = 18.dp, vertical = 12.dp)
                            .animateContentSize(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(entry.icon(), contentDescription = if (on) null else stringResource(entry.label), tint = tint)
                        if (on) {
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(entry.label), color = tint, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
    }
}
