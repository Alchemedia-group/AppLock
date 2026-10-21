package com.alchemedia.applock.ui.main

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import com.alchemedia.applock.R
import com.alchemedia.applock.data.AppSettings

private enum class Tab(val label: Int, val icon: ImageVector) {
    Apps(R.string.tab_apps, Icons.Default.Lock),
    Intruders(R.string.tab_intruders, Icons.Default.Face),
    Settings(R.string.tab_settings, Icons.Default.Settings),
}

@Composable
fun HomeScreen(settings: AppSettings, viewModel: MainViewModel) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var changingCredential by rememberSaveable { mutableStateOf(false) }

    val apps by viewModel.apps.collectAsState()
    val shots by viewModel.shots.collectAsState()
    val statusState = rememberSetupStatus()
    val status by statusState

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.loadApps()
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

    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEachIndexed { index, entry ->
                    NavigationBarItem(
                        selected = tab == index,
                        onClick = { tab = index },
                        icon = { Icon(entry.icon, contentDescription = null) },
                        label = { Text(stringResource(entry.label)) },
                    )
                }
            }
        },
    ) { padding ->
        val content = Modifier.padding(padding)
        when (Tab.entries[tab]) {
            Tab.Apps -> AppsTab(
                settings = settings,
                apps = apps,
                status = status,
                onToggle = viewModel::setLocked,
                modifier = content,
            )
            Tab.Intruders -> IntrudersTab(
                shots = shots,
                onDelete = viewModel::deleteShot,
                onDeleteAll = viewModel::deleteAllShots,
                modifier = content,
            )
            Tab.Settings -> SettingsTab(
                settings = settings,
                status = status,
                onChangeCredential = { changingCredential = true },
                onBiometric = { viewModel.setBiometricEnabled(it) },
                onShuffle = { viewModel.setShuffleKeypad(it) },
                onCapture = { viewModel.setCaptureIntruders(it) },
                onAttempts = { viewModel.setAttemptsBeforeCapture(it) },
                onRelock = { viewModel.setRelockMode(it) },
                modifier = content,
            )
        }
    }
}
