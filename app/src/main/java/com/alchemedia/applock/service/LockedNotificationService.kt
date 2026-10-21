package com.alchemedia.applock.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.alchemedia.applock.AppLockApp
import com.alchemedia.applock.R
import com.alchemedia.applock.data.AppSettings
import com.alchemedia.applock.security.UnlockSession
import com.alchemedia.applock.security.canPostNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Replaces notifications from locked apps with a generic "New notification" so their contents never
 * show on the lock screen or shade. Tapping the replacement opens the app, which then asks to unlock.
 */
class LockedNotificationService : NotificationListenerService() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var settings = AppSettings()

    override fun onListenerConnected() {
        super.onListenerConnected()
        createChannel()
        scope.launch {
            (application as AppLockApp).settingsRepository.settings.collect { settings = it }
        }
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (sbn == null) return
        val s = settings
        val pkg = sbn.packageName
        if (!s.setupComplete || !s.hideNotifications) return
        if (pkg == packageName || pkg !in s.lockedPackages) return
        if (UnlockSession.isUnlocked(pkg, s.relockMode)) return
        if (!isHideable(sbn.notification)) return
        // Never drop the original unless the replacement can actually be shown.
        if (!canPost()) return

        val label = try {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        } catch (_: PackageManager.NameNotFoundException) {
            pkg
        }
        val launch = packageManager.getLaunchIntentForPackage(pkg)
        val tap = launch?.let {
            PendingIntent.getActivity(this, pkg.hashCode(), it, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val replacement = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(label)
            .setContentText(getString(R.string.hidden_notification_text))
            .setContentIntent(tap)
            .setAutoCancel(true)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .build()
        try {
            NotificationManagerCompat.from(this).notify(pkg.hashCode(), replacement)
            cancelNotification(sbn.key)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post; leave the original alone.
        }
    }

    /** Ongoing, foreground-service, call and alarm notifications must stay as they are. */
    private fun isHideable(n: Notification): Boolean {
        val sticky = Notification.FLAG_ONGOING_EVENT or Notification.FLAG_FOREGROUND_SERVICE or Notification.FLAG_NO_CLEAR
        if (n.flags and sticky != 0) return false
        return n.category != Notification.CATEGORY_CALL && n.category != Notification.CATEGORY_ALARM
    }

    private fun canPost(): Boolean =
        NotificationManagerCompat.from(this).areNotificationsEnabled() && canPostNotifications(this)

    private fun createChannel() {
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.hidden_notification_channel), NotificationManager.IMPORTANCE_DEFAULT)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "locked_app_notifications"
    }
}
