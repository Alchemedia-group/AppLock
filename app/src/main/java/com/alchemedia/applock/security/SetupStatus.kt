package com.alchemedia.applock.security

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.text.TextUtils
import androidx.biometric.BiometricManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.alchemedia.applock.service.AppLockAccessibilityService

data class SetupStatus(
    val accessibilityEnabled: Boolean,
    val cameraGranted: Boolean,
    val notificationListenerEnabled: Boolean,
    val postNotificationsGranted: Boolean,
) {
    companion object {
        fun read(context: Context) = SetupStatus(
            accessibilityEnabled = isAccessibilityServiceEnabled(context),
            cameraGranted = hasCameraPermission(context),
            notificationListenerEnabled = isNotificationListenerEnabled(context),
            postNotificationsGranted = canPostNotifications(context),
        )
    }
}

fun hasCameraPermission(context: Context): Boolean =
    ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val ours = ComponentName(context, AppLockAccessibilityService::class.java)
    val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        ?: return false
    val splitter = TextUtils.SimpleStringSplitter(':').apply { setString(enabled) }
    while (splitter.hasNext()) {
        if (ComponentName.unflattenFromString(splitter.next()) == ours) return true
    }
    return false
}

fun isNotificationListenerEnabled(context: Context): Boolean =
    NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

/** Notification permission is only a runtime permission from Android 13; before that it is implicit. */
fun canPostNotifications(context: Context): Boolean =
    Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

fun isBiometricAvailable(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS
