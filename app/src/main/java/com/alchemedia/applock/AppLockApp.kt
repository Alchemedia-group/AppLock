package com.alchemedia.applock

import android.app.Application
import com.alchemedia.applock.billing.BillingManager
import com.alchemedia.applock.data.SettingsRepository
import com.alchemedia.applock.intruder.IntruderRepository

class AppLockApp : Application() {
    val settingsRepository by lazy { SettingsRepository(this) }
    val intruderRepository by lazy { IntruderRepository(this) }
    val billingManager by lazy { BillingManager(this, settingsRepository) }
}
