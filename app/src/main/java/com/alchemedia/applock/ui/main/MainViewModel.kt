package com.alchemedia.applock.ui.main

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alchemedia.applock.AppLockApp
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.data.LockType
import com.alchemedia.applock.data.RelockMode
import com.alchemedia.applock.intruder.IntruderShot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val appLock = app as AppLockApp
    private val repository = appLock.settingsRepository
    private val intruders = appLock.intruderRepository

    /** Null until the first read from disk completes. */
    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    val shots: StateFlow<List<IntruderShot>> = intruders.shots

    /** True once the user has passed AppLock's own lock screen; reset when the app leaves the foreground. */
    var authenticated by mutableStateOf(false)

    private val _apps = MutableStateFlow<List<AppEntry>?>(null)
    val apps: StateFlow<List<AppEntry>?> = _apps.asStateFlow()

    fun loadApps() {
        viewModelScope.launch {
            _apps.value = withContext(Dispatchers.Default) { AppLoader.load(getApplication()) }
        }
    }

    fun refreshShots() = intruders.refresh()
    fun deleteShot(shot: IntruderShot) = viewModelScope.launch(Dispatchers.IO) { intruders.delete(shot) }
    fun deleteAllShots() = viewModelScope.launch(Dispatchers.IO) { intruders.deleteAll() }

    fun setLocked(packageName: String, locked: Boolean) {
        viewModelScope.launch { repository.setLocked(packageName, locked) }
    }

    fun setCredential(type: LockType, secret: String) {
        viewModelScope.launch {
            repository.setCredential(type, secret)
            authenticated = true
        }
    }

    fun setBiometricEnabled(value: Boolean) = viewModelScope.launch { repository.setBiometricEnabled(value) }
    fun setCaptureIntruders(value: Boolean) = viewModelScope.launch { repository.setCaptureIntruders(value) }
    fun setAttemptsBeforeCapture(value: Int) = viewModelScope.launch { repository.setAttemptsBeforeCapture(value) }
    fun setRelockMode(value: RelockMode) = viewModelScope.launch { repository.setRelockMode(value) }
    fun setShuffleKeypad(value: Boolean) = viewModelScope.launch { repository.setShuffleKeypad(value) }
}
