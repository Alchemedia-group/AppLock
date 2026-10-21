package com.alchemedia.applock.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

/**
 * The language the user picked inside AppLock. It lives in plain SharedPreferences (not DataStore)
 * because it has to be read synchronously in attachBaseContext, before any screen exists.
 * An empty tag means "follow the phone's language".
 */
object LanguageManager {
    /** Languages AppLock is translated into; must match res/xml/locales_config.xml. */
    val SUPPORTED = listOf("en", "es", "de", "fr", "it", "pt", "pl")

    private const val PREFS = "language"
    private const val KEY_TAG = "tag"
    private const val KEY_CHOSEN = "chosen"

    /** The saved language tag, or "" to follow the phone. */
    fun tag(context: Context): String =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_TAG, "").orEmpty()

    /** False until the user has answered the first-launch language question. */
    fun isChosen(context: Context): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean(KEY_CHOSEN, false)

    fun save(context: Context, tag: String) {
        val safe = if (tag in SUPPORTED) tag else ""
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TAG, safe)
            .putBoolean(KEY_CHOSEN, true)
            .apply()
    }

    /** Returns [base] with the saved language applied; call from attachBaseContext of every activity and service. */
    fun wrap(base: Context): Context {
        val tag = tag(base)
        if (tag.isEmpty()) {
            // Back to the phone's own language: undo any default set earlier in this process.
            Locale.setDefault(Resources.getSystem().configuration.locales[0])
            return base
        }
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return base.createConfigurationContext(config)
    }
}
