package com.alchemedia.applock.data

enum class LockType { PIN, PATTERN }

enum class RelockMode { IMMEDIATE, ONE_MINUTE, FIVE_MINUTES, SCREEN_OFF }

data class AppSettings(
    val setupComplete: Boolean = false,
    val lockType: LockType = LockType.PIN,
    /** Base64 PBKDF2 hash of the PIN / pattern; null until setup is complete. */
    val credentialHash: String? = null,
    val credentialSalt: String? = null,
    val lockedPackages: Set<String> = emptySet(),
    val biometricEnabled: Boolean = false,
    val captureIntruders: Boolean = true,
    val attemptsBeforeCapture: Int = 1,
    val relockMode: RelockMode = RelockMode.IMMEDIATE,
    val shuffleKeypad: Boolean = false,
    /** Base64 PBKDF2 hash of the one-time recovery code; null if none has been created. */
    val recoveryHash: String? = null,
    val recoverySalt: String? = null,
    val fakeCrash: Boolean = false,
    val hideNotifications: Boolean = false,
    /** Delete intruder photos older than this many days; 0 keeps them forever. */
    val autoDeleteDays: Int = 0,
    /** True once the one-time Pro purchase has been verified with Google Play. */
    val proUnlocked: Boolean = false,
)
