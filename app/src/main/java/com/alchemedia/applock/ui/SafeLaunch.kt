package com.alchemedia.applock.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.annotation.StringRes
import com.alchemedia.applock.R

/**
 * Opening another screen (a Settings page, the file picker, the share sheet) can fail on phones that
 * lack it, for example a ROM without a document picker. These helpers show a message instead of crashing.
 */
fun Context.startActivitySafely(intent: Intent, @StringRes errorRes: Int = R.string.error_action_unavailable): Boolean =
    try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        showUnavailable(errorRes)
        false
    } catch (_: SecurityException) {
        showUnavailable(errorRes)
        false
    }

/** Same idea for ActivityResult launchers such as the file picker and permission requests. */
fun <I> ActivityResultLauncher<I>.launchSafely(
    context: Context,
    input: I,
    @StringRes errorRes: Int = R.string.error_action_unavailable,
): Boolean =
    try {
        launch(input)
        true
    } catch (_: ActivityNotFoundException) {
        context.showUnavailable(errorRes)
        false
    } catch (_: SecurityException) {
        context.showUnavailable(errorRes)
        false
    }

private fun Context.showUnavailable(@StringRes errorRes: Int) {
    Toast.makeText(this, errorRes, Toast.LENGTH_LONG).show()
}
