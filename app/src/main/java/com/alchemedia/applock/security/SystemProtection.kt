package com.alchemedia.applock.security

import com.alchemedia.applock.data.AppSettings

/**
 * Optional protection for AppLock itself. Every uninstall (from Settings, Google Play or the home screen)
 * ends in the same system confirmation dialog, which belongs to the phone's package installer, so putting
 * the PIN screen in front of that dialog covers all of those routes. Locking Settings stops someone
 * switching AppLock's service off there first.
 */
object SystemProtection {
    /** Package installer / uninstaller apps used by common Android builds. Only those present on the phone ever fire events. */
    val INSTALLERS = setOf(
        "com.google.android.packageinstaller",
        "com.android.packageinstaller",
        "com.samsung.android.packageinstaller",
        "com.miui.packageinstaller",
    )

    val SETTINGS = setOf("com.android.settings")

    fun isInstaller(packageName: String) = packageName in INSTALLERS

    /** True for the system apps AppLock can guard on the user's behalf (not chosen from the app list). */
    fun isSystemGuard(packageName: String) = packageName in INSTALLERS || packageName in SETTINGS

    /** Whether opening [packageName] should show the lock screen under the current [settings]. */
    fun isLocked(packageName: String, settings: AppSettings): Boolean =
        packageName in settings.lockedPackages ||
            (settings.protectUninstall && packageName in INSTALLERS) ||
            (settings.protectSettings && packageName in SETTINGS)
}
