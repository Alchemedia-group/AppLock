package com.alchemedia.applock.security

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.provider.Settings
import android.text.TextUtils
import androidx.biometric.BiometricManager
import androidx.core.content.ContextCompat
import com.alchemedia.applock.service.AppLockAccessibilityService

data class SetupStatus(val accessibilityEnabled: Boolean, val cameraGranted: Boolean) {
    companion object {
        fun read(context: Context) = SetupStatus(
            accessibilityEnabled = isAccessibilityServiceEnabled(context),
            cameraGranted = hasCameraPermission(context),
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

fun isBiometricAvailable(context: Context): Boolean =
    BiometricManager.from(context).canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
        BiometricManager.BIOMETRIC_SUCCESS
