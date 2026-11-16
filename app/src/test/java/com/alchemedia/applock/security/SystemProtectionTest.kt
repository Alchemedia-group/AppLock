package com.alchemedia.applock.security

import com.alchemedia.applock.data.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SystemProtectionTest {
    private val installer = "com.google.android.packageinstaller"
    private val settingsApp = "com.android.settings"

    @Test
    fun nothingSystemIsLockedByDefault() {
        val s = AppSettings()
        assertFalse(SystemProtection.isLocked(installer, s))
        assertFalse(SystemProtection.isLocked(settingsApp, s))
    }

    @Test
    fun uninstallProtectionLocksEveryInstallerButNotSettings() {
        val s = AppSettings(protectUninstall = true)
        SystemProtection.INSTALLERS.forEach { assertTrue(it, SystemProtection.isLocked(it, s)) }
        assertFalse(SystemProtection.isLocked(settingsApp, s))
    }

    @Test
    fun settingsProtectionLocksSettingsButNotInstallers() {
        val s = AppSettings(protectSettings = true)
        assertTrue(SystemProtection.isLocked(settingsApp, s))
        assertFalse(SystemProtection.isLocked(installer, s))
    }

    @Test
    fun userChosenAppsStillLockWithBothSwitchesOff() {
        val s = AppSettings(lockedPackages = setOf("com.bank"))
        assertTrue(SystemProtection.isLocked("com.bank", s))
        assertFalse(SystemProtection.isLocked("com.other", s))
    }

    @Test
    fun systemGuardsAreRecognised() {
        assertTrue(SystemProtection.isSystemGuard(installer))
        assertTrue(SystemProtection.isSystemGuard(settingsApp))
        assertFalse(SystemProtection.isSystemGuard("com.bank"))
        assertTrue(SystemProtection.isInstaller(installer))
        assertFalse(SystemProtection.isInstaller(settingsApp))
    }
}
