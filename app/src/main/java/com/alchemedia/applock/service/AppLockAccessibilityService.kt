package com.alchemedia.applock.service

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.view.accessibility.AccessibilityEvent
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import com.alchemedia.applock.AppLockApp
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.data.RelockMode
import com.alchemedia.applock.security.UnlockSession
import com.alchemedia.applock.ui.lock.LockActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Watches window changes to learn which app is in the foreground and raises [LockActivity] over
 * protected apps that have not been unlocked. It never inspects screen content.
 */
class AppLockAccessibilityService : AccessibilityService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var settings = AppSettings()
    private var lastPackage: String? = null
    private var imePackages: Set<String> = emptySet()
    private var receiverRegistered = false

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                Intent.ACTION_SCREEN_OFF -> UnlockSession.clear()
                // After the keyguard is dismissed the foreground app may not emit a new window event.
                Intent.ACTION_USER_PRESENT -> lastPackage?.let { evaluate(it) }
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        imePackages = loadImePackages()
        scope.launch {
            (application as AppLockApp).settingsRepository.settings.collect { settings = it }
        }
        scope.launch {
            // Housekeeping at connect time, so old photos go even if the app itself is rarely opened.
            val app = application as AppLockApp
            val days = app.settingsRepository.settings.first().autoDeleteDays
            withContext(Dispatchers.IO) { app.intruderRepository.purgeOlderThan(days) }
        }
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            }
            ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
            receiverRegistered = true
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null || event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg in IGNORED_PACKAGES || pkg in imePackages) return
        if (pkg == lastPackage) return

        lastPackage = pkg
        if (settings.relockMode == RelockMode.IMMEDIATE) UnlockSession.retainOnly(pkg)
        evaluate(pkg)
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        if (receiverRegistered) {
            unregisterReceiver(screenReceiver)
            receiverRegistered = false
        }
        scope.cancel()
        super.onDestroy()
    }

    private fun evaluate(pkg: String) {
        val s = settings
        if (!s.setupComplete || pkg !in s.lockedPackages) return
        if (UnlockSession.isUnlocked(pkg, s.relockMode)) return
        showLock(pkg)
    }

    private fun showLock(pkg: String) {
        val intent = Intent(this, LockActivity::class.java)
            .putExtra(LockActivity.EXTRA_PACKAGE, pkg)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_NO_ANIMATION)
        startActivity(intent)
    }

    private fun loadImePackages(): Set<String> =
        getSystemService(InputMethodManager::class.java)
            ?.enabledInputMethodList
            ?.map { it.packageName }
            ?.toSet()
            .orEmpty()

    private companion object {
        /** System surfaces that overlay apps without meaning the user left them. */
        val IGNORED_PACKAGES = setOf(
            "android",
            "com.android.systemui",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
        )
    }
}
