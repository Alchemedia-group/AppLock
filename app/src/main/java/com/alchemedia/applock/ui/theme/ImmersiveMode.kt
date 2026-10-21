package com.alchemedia.applock.ui.theme

import android.view.Window
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Keeps the Android navigation bar hidden. A swipe from the screen edge reveals it transiently and
 * it hides itself again. Call from onCreate and onWindowFocusChanged(true) of every activity.
 */
object ImmersiveMode {
    fun apply(window: Window) {
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.navigationBars())
    }
}
