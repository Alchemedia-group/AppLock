package com.alchemedia.applock.ui.lock

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.alchemedia.applock.AppLockApp
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.security.CredentialHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class LockUiState(
    val wrongAttempt: Boolean = false,
    /** Running failure count; changes on every failure so the UI can replay its shake animation. */
    val attempt: Int = 0,
    /** [SystemClock.elapsedRealtime] timestamp until which input is refused, or 0. */
    val lockedUntil: Long = 0L,
)

/**
 * Shared by the app-lock overlay and the AppLock app's own gate: verifies the secret, counts
 * failures, enforces a short lockout, and asks the host activity to take an intruder photo.
 */
class LockViewModel(app: Application) : AndroidViewModel(app) {
    private val repository = (app as AppLockApp).settingsRepository

    val settings: StateFlow<AppSettings?> =
        repository.settings.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _ui = MutableStateFlow(LockUiState())
    val ui: StateFlow<LockUiState> = _ui.asStateFlow()

    private val _captureRequests = MutableSharedFlow<Int>(extraBufferCapacity = 4)

    /** Emits the running failure count whenever a photo should be taken. */
    val captureRequests: SharedFlow<Int> = _captureRequests.asSharedFlow()

    private var failures = 0

    fun submit(secret: String, onSuccess: () -> Unit) {
        val s = settings.value ?: return
        if (SystemClock.elapsedRealtime() < _ui.value.lockedUntil) return
        viewModelScope.launch {
            val ok = withContext(Dispatchers.Default) {
                CredentialHasher.verify(secret, s.credentialSalt, s.credentialHash)
            }
            if (ok) {
                failures = 0
                _ui.value = LockUiState()
                onSuccess()
            } else {
                registerFailure()
            }
        }
    }

    /** Also used for biometric mismatches, which count as failed attempts too. */
    fun registerFailure() {
        val s = settings.value ?: return
        failures++
        if (s.captureIntruders && failures % s.attemptsBeforeCapture == 0) {
            _captureRequests.tryEmit(failures)
        }
        val lockedUntil = if (failures % LOCKOUT_EVERY == 0) {
            SystemClock.elapsedRealtime() + LOCKOUT_MS
        } else {
            0L
        }
        _ui.value = LockUiState(wrongAttempt = true, attempt = failures, lockedUntil = lockedUntil)
    }

    fun onSuccessfulBiometric() {
        failures = 0
        _ui.value = LockUiState()
    }

    companion object {
        const val LOCKOUT_EVERY = 5
        const val LOCKOUT_MS = 30_000L
    }
}
