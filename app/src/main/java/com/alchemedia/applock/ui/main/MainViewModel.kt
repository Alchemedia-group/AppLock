package com.alchemedia.applock.ui.main

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alchemedia.applock.AppLockApp
import com.alchemedia.applock.R
import com.alchemedia.applock.data.BackupManager
import com.alchemedia.applock.security.CredentialHasher
import com.alchemedia.applock.security.RecoveryCode
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

    /** True while the accessibility disclosure screen should be on top of everything. */
    var accessibilityDisclosurePending by mutableStateOf(false)

    fun agreeToAccessibilityDisclosure() {
        accessibilityDisclosurePending = false
        viewModelScope.launch { repository.setAccessibilityConsent(true) }
    }

    private val billing = appLock.billingManager
    val billingState = billing.state

    fun startBilling() = billing.start()
    fun purchasePro(activity: android.app.Activity) = billing.purchase(activity)
    fun restorePurchases() = billing.restore()
    fun clearBillingMessage() = billing.clearMessage()
    fun debugUnlockPro() = billing.debugUnlock()

    fun refreshShots() = intruders.refresh()
    fun deleteShot(shot: IntruderShot) = viewModelScope.launch(Dispatchers.IO) { intruders.delete(shot) }
    fun deleteAllShots() = viewModelScope.launch(Dispatchers.IO) { intruders.deleteAll() }

    fun setLocked(packageName: String, locked: Boolean) {
        viewModelScope.launch { repository.setLocked(packageName, locked) }
    }

    /** Saves a new PIN/pattern; [authenticate] also marks the user as having passed AppLock's own gate. */
    fun setCredential(type: LockType, secret: String, authenticate: Boolean = true) {
        viewModelScope.launch {
            repository.setCredential(type, secret)
            if (authenticate) authenticated = true
        }
    }

    fun completeSetup() {
        viewModelScope.launch {
            repository.completeSetup()
            authenticated = true
        }
    }

    fun saveRecoveryCode(code: String) {
        viewModelScope.launch { repository.setRecoveryCode(code) }
    }

    fun verifyRecovery(code: String, onResult: (Boolean) -> Unit) {
        val s = settings.value
        if (s == null) {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) {
                CredentialHasher.verify(RecoveryCode.normalize(code), s.recoverySalt, s.recoveryHash)
            }
            onResult(ok)
        }
    }

    fun purgeOldShots(days: Int) {
        if (days <= 0) return
        viewModelScope.launch(Dispatchers.IO) { intruders.purgeOlderThan(days) }
    }

    /** String resource describing the last backup/restore outcome, or null. */
    private val _backupMessage = MutableStateFlow<Int?>(null)
    val backupMessage: StateFlow<Int?> = _backupMessage.asStateFlow()

    fun clearBackupMessage() {
        _backupMessage.value = null
    }

    fun exportBackup(uri: Uri) {
        val s = settings.value ?: return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.use {
                        it.write(BackupManager.toJson(s).toByteArray())
                    } != null
                }.getOrDefault(false)
            }
            _backupMessage.value = if (ok) R.string.backup_saved else R.string.backup_failed
        }
    }

    fun importBackup(uri: Uri) {
        viewModelScope.launch {
            val parsed = withContext(Dispatchers.IO) {
                runCatching {
                    getApplication<Application>().contentResolver.openInputStream(uri)?.use {
                        BackupManager.fromJson(it.readBytes().decodeToString())
                    }
                }.getOrNull()
            }
            if (parsed == null) {
                _backupMessage.value = R.string.backup_invalid
            } else {
                repository.restore(parsed)
                _backupMessage.value = R.string.backup_restored
            }
        }
    }

    fun setBiometricEnabled(value: Boolean) = viewModelScope.launch { repository.setBiometricEnabled(value) }
    fun setCaptureIntruders(value: Boolean) = viewModelScope.launch { repository.setCaptureIntruders(value) }
    fun setAttemptsBeforeCapture(value: Int) = viewModelScope.launch { repository.setAttemptsBeforeCapture(value) }
    fun setRelockMode(value: RelockMode) = viewModelScope.launch { repository.setRelockMode(value) }
    fun setShuffleKeypad(value: Boolean) = viewModelScope.launch { repository.setShuffleKeypad(value) }
    fun setFakeCrash(value: Boolean) = viewModelScope.launch { repository.setFakeCrash(value) }
    fun setHideNotifications(value: Boolean) = viewModelScope.launch { repository.setHideNotifications(value) }
    fun setAutoDeleteDays(value: Int) = viewModelScope.launch { repository.setAutoDeleteDays(value) }
}
