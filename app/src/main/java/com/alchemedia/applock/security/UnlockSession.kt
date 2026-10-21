package com.alchemedia.applock.security

import android.os.SystemClock
import com.alchemedia.applock.data.RelockMode
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory record of which protected apps the user has unlocked. Shared between the lock screen
 * and the accessibility service; intentionally never persisted so a process restart re-locks everything.
 */
object UnlockSession {
    private val unlockedAt = ConcurrentHashMap<String, Long>()

    fun markUnlocked(packageName: String) {
        unlockedAt[packageName] = SystemClock.elapsedRealtime()
    }

    fun isUnlocked(packageName: String, mode: RelockMode): Boolean {
        val at = unlockedAt[packageName] ?: return false
        val grace = when (mode) {
            RelockMode.IMMEDIATE, RelockMode.SCREEN_OFF -> return true
            RelockMode.ONE_MINUTE -> 60_000L
            RelockMode.FIVE_MINUTES -> 300_000L
        }
        return SystemClock.elapsedRealtime() - at < grace
    }

    /** Forget every unlocked app except [packageName] (the one now in the foreground). */
    fun retainOnly(packageName: String?) {
        unlockedAt.keys.filter { it != packageName }.forEach { unlockedAt.remove(it) }
    }

    fun clear() = unlockedAt.clear()
}
