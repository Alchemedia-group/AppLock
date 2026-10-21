package com.alchemedia.applock.data

import android.content.Context
import androidx.datastore.preferences.core.MutablePreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.alchemedia.applock.security.CredentialHasher
import com.alchemedia.applock.security.RecoveryCode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

private val Context.settingsStore by preferencesDataStore(name = "applock_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val SETUP_COMPLETE = booleanPreferencesKey("setup_complete")
        val LOCK_TYPE = stringPreferencesKey("lock_type")
        val CREDENTIAL_HASH = stringPreferencesKey("credential_hash")
        val CREDENTIAL_SALT = stringPreferencesKey("credential_salt")
        val LOCKED_PACKAGES = stringSetPreferencesKey("locked_packages")
        val BIOMETRIC = booleanPreferencesKey("biometric_enabled")
        val CAPTURE = booleanPreferencesKey("capture_intruders")
        val ATTEMPTS = intPreferencesKey("attempts_before_capture")
        val RELOCK = stringPreferencesKey("relock_mode")
        val SHUFFLE = booleanPreferencesKey("shuffle_keypad")
        val RECOVERY_HASH = stringPreferencesKey("recovery_hash")
        val RECOVERY_SALT = stringPreferencesKey("recovery_salt")
        val FAKE_CRASH = booleanPreferencesKey("fake_crash")
        val HIDE_NOTIFICATIONS = booleanPreferencesKey("hide_notifications")
        val AUTO_DELETE_DAYS = intPreferencesKey("auto_delete_days")
        val PRO_UNLOCKED = booleanPreferencesKey("pro_unlocked")
        val ACCESSIBILITY_CONSENT = booleanPreferencesKey("accessibility_consent")
    }

    val settings: Flow<AppSettings> = context.settingsStore.data.map { it.toSettings() }

    suspend fun setLocked(packageName: String, locked: Boolean) = update { prefs ->
        val current = prefs[Keys.LOCKED_PACKAGES].orEmpty()
        prefs[Keys.LOCKED_PACKAGES] = if (locked) current + packageName else current - packageName
    }

    /** Hashes [secret] with a fresh salt and stores it. Setup is only complete once [completeSetup] runs. */
    suspend fun setCredential(type: LockType, secret: String) {
        val salt = CredentialHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { CredentialHasher.hash(secret, salt) }
        update { prefs ->
            prefs[Keys.LOCK_TYPE] = type.name
            prefs[Keys.CREDENTIAL_SALT] = CredentialHasher.encode(salt)
            prefs[Keys.CREDENTIAL_HASH] = CredentialHasher.encode(hash)
        }
    }

    suspend fun completeSetup() = update { it[Keys.SETUP_COMPLETE] = true }

    /** Stores a salted hash of [code] (normalised) so it can unlock a forgotten PIN or pattern. */
    suspend fun setRecoveryCode(code: String) {
        val salt = CredentialHasher.newSalt()
        val hash = withContext(Dispatchers.Default) { CredentialHasher.hash(RecoveryCode.normalize(code), salt) }
        update { prefs ->
            prefs[Keys.RECOVERY_SALT] = CredentialHasher.encode(salt)
            prefs[Keys.RECOVERY_HASH] = CredentialHasher.encode(hash)
        }
    }

    /** Applies the non-secret preferences from a restored backup. Credentials and recovery code are never touched. */
    suspend fun restore(backup: AppSettings) = update { prefs ->
        prefs[Keys.LOCKED_PACKAGES] = backup.lockedPackages
        prefs[Keys.BIOMETRIC] = backup.biometricEnabled
        prefs[Keys.CAPTURE] = backup.captureIntruders
        prefs[Keys.ATTEMPTS] = backup.attemptsBeforeCapture.coerceIn(1, 5)
        prefs[Keys.RELOCK] = backup.relockMode.name
        prefs[Keys.SHUFFLE] = backup.shuffleKeypad
        prefs[Keys.FAKE_CRASH] = backup.fakeCrash
        prefs[Keys.HIDE_NOTIFICATIONS] = backup.hideNotifications
        prefs[Keys.AUTO_DELETE_DAYS] = backup.autoDeleteDays
    }

    suspend fun setBiometricEnabled(value: Boolean) = update { it[Keys.BIOMETRIC] = value }
    suspend fun setCaptureIntruders(value: Boolean) = update { it[Keys.CAPTURE] = value }
    suspend fun setAttemptsBeforeCapture(value: Int) = update { it[Keys.ATTEMPTS] = value.coerceIn(1, 5) }
    suspend fun setRelockMode(value: RelockMode) = update { it[Keys.RELOCK] = value.name }
    suspend fun setShuffleKeypad(value: Boolean) = update { it[Keys.SHUFFLE] = value }
    suspend fun setFakeCrash(value: Boolean) = update { it[Keys.FAKE_CRASH] = value }
    suspend fun setHideNotifications(value: Boolean) = update { it[Keys.HIDE_NOTIFICATIONS] = value }
    suspend fun setAccessibilityConsent(value: Boolean) = update { it[Keys.ACCESSIBILITY_CONSENT] = value }
    suspend fun setProUnlocked(value: Boolean) = update { it[Keys.PRO_UNLOCKED] = value }
    suspend fun setAutoDeleteDays(value: Int) = update { it[Keys.AUTO_DELETE_DAYS] = value.coerceAtLeast(0) }

    private suspend fun update(block: (MutablePreferences) -> Unit) {
        context.settingsStore.edit { block(it) }
    }

    private fun Preferences.toSettings() = AppSettings(
        setupComplete = this[Keys.SETUP_COMPLETE] ?: false,
        lockType = enumOrDefault(this[Keys.LOCK_TYPE], LockType.PIN),
        credentialHash = this[Keys.CREDENTIAL_HASH],
        credentialSalt = this[Keys.CREDENTIAL_SALT],
        lockedPackages = this[Keys.LOCKED_PACKAGES].orEmpty(),
        biometricEnabled = this[Keys.BIOMETRIC] ?: false,
        captureIntruders = this[Keys.CAPTURE] ?: true,
        attemptsBeforeCapture = this[Keys.ATTEMPTS] ?: 1,
        relockMode = enumOrDefault(this[Keys.RELOCK], RelockMode.IMMEDIATE),
        shuffleKeypad = this[Keys.SHUFFLE] ?: false,
        recoveryHash = this[Keys.RECOVERY_HASH],
        recoverySalt = this[Keys.RECOVERY_SALT],
        fakeCrash = this[Keys.FAKE_CRASH] ?: false,
        hideNotifications = this[Keys.HIDE_NOTIFICATIONS] ?: false,
        autoDeleteDays = this[Keys.AUTO_DELETE_DAYS] ?: 0,
        proUnlocked = this[Keys.PRO_UNLOCKED] ?: false,
        accessibilityConsent = this[Keys.ACCESSIBILITY_CONSENT] ?: false,
    )

    private inline fun <reified E : Enum<E>> enumOrDefault(name: String?, default: E): E =
        name?.let { n -> enumValues<E>().firstOrNull { it.name == n } } ?: default
}
