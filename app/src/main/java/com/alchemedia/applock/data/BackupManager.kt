package com.alchemedia.applock.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * Settings backup as a small JSON document. It covers which apps are locked and the preferences,
 * and deliberately leaves out the PIN/pattern, the recovery code and intruder photos.
 */
object BackupManager {
    private const val VERSION = 1

    fun toJson(settings: AppSettings): String = JSONObject().apply {
        put("app", "applock")
        put("version", VERSION)
        put("lockedPackages", JSONArray(settings.lockedPackages.sorted()))
        put("biometricEnabled", settings.biometricEnabled)
        put("captureIntruders", settings.captureIntruders)
        put("attemptsBeforeCapture", settings.attemptsBeforeCapture)
        put("relockMode", settings.relockMode.name)
        put("shuffleKeypad", settings.shuffleKeypad)
        put("fakeCrash", settings.fakeCrash)
        put("hideNotifications", settings.hideNotifications)
        put("autoDeleteDays", settings.autoDeleteDays)
    }.toString(2)

    /** Returns null if [json] isn't an AppLock backup. Missing fields fall back to defaults. */
    fun fromJson(json: String): AppSettings? = try {
        val o = JSONObject(json)
        if (o.optString("app") != "applock" || o.optInt("version", -1) !in 1..VERSION) {
            null
        } else {
            val locked = o.optJSONArray("lockedPackages")
            val defaults = AppSettings()
            defaults.copy(
                lockedPackages = buildSet {
                    if (locked != null) for (i in 0 until locked.length()) add(locked.getString(i))
                },
                biometricEnabled = o.optBoolean("biometricEnabled", defaults.biometricEnabled),
                captureIntruders = o.optBoolean("captureIntruders", defaults.captureIntruders),
                attemptsBeforeCapture = o.optInt("attemptsBeforeCapture", defaults.attemptsBeforeCapture).coerceIn(1, 5),
                relockMode = RelockMode.entries.firstOrNull { it.name == o.optString("relockMode") } ?: defaults.relockMode,
                shuffleKeypad = o.optBoolean("shuffleKeypad", defaults.shuffleKeypad),
                fakeCrash = o.optBoolean("fakeCrash", defaults.fakeCrash),
                hideNotifications = o.optBoolean("hideNotifications", defaults.hideNotifications),
                autoDeleteDays = o.optInt("autoDeleteDays", defaults.autoDeleteDays).coerceAtLeast(0),
            )
        }
    } catch (_: JSONException) {
        null
    }
}
