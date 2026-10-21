package com.alchemedia.applock.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BackupManagerTest {
    private val sample = AppSettings(
        setupComplete = true,
        credentialHash = "secret-hash",
        credentialSalt = "secret-salt",
        recoveryHash = "recovery-hash",
        recoverySalt = "recovery-salt",
        lockedPackages = setOf("com.bank", "com.chat"),
        biometricEnabled = true,
        captureIntruders = false,
        attemptsBeforeCapture = 3,
        relockMode = RelockMode.FIVE_MINUTES,
        shuffleKeypad = true,
        fakeCrash = true,
        hideNotifications = true,
        autoDeleteDays = 30,
    )

    @Test
    fun roundTripPreservesPreferences() {
        val restored = BackupManager.fromJson(BackupManager.toJson(sample))
        assertNotNull(restored)
        restored!!
        assertEquals(sample.lockedPackages, restored.lockedPackages)
        assertEquals(sample.biometricEnabled, restored.biometricEnabled)
        assertEquals(sample.captureIntruders, restored.captureIntruders)
        assertEquals(3, restored.attemptsBeforeCapture)
        assertEquals(RelockMode.FIVE_MINUTES, restored.relockMode)
        assertEquals(true, restored.shuffleKeypad)
        assertEquals(true, restored.fakeCrash)
        assertEquals(true, restored.hideNotifications)
        assertEquals(30, restored.autoDeleteDays)
    }

    @Test
    fun backupNeverContainsSecrets() {
        val json = BackupManager.toJson(sample)
        assertFalse(json.contains("secret-hash"))
        assertFalse(json.contains("secret-salt"))
        assertFalse(json.contains("recovery-hash"))
        assertFalse(json.contains("recovery-salt"))
    }

    @Test
    fun restoredSettingsCarryNoCredentials() {
        val restored = BackupManager.fromJson(BackupManager.toJson(sample))!!
        assertNull(restored.credentialHash)
        assertNull(restored.recoveryHash)
    }

    @Test
    fun rejectsGarbageAndForeignJson() {
        assertNull(BackupManager.fromJson("not json"))
        assertNull(BackupManager.fromJson("{}"))
        assertNull(BackupManager.fromJson("""{"app":"other","version":1}"""))
        assertNull(BackupManager.fromJson("""{"app":"applock","version":99}"""))
    }

    @Test
    fun missingFieldsFallBackToDefaultsAndValuesAreClamped() {
        val restored = BackupManager.fromJson(
            """{"app":"applock","version":1,"attemptsBeforeCapture":99,"autoDeleteDays":-5,"relockMode":"NOPE"}""",
        )!!
        assertEquals(5, restored.attemptsBeforeCapture)
        assertEquals(0, restored.autoDeleteDays)
        assertEquals(RelockMode.IMMEDIATE, restored.relockMode)
        assertEquals(emptySet<String>(), restored.lockedPackages)
    }
}
