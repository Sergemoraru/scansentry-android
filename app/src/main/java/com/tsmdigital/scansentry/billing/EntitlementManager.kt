package com.tsmdigital.scansentry.billing

import android.content.Context

/**
 * Placeholder entitlement state.
 * We'll wire this to Google Play Billing (monthly/yearly base plans) next.
 */
class EntitlementManager(context: Context) {
    private val prefs = context.getSharedPreferences("entitlements", Context.MODE_PRIVATE)

    var isPro: Boolean
        get() = prefs.getBoolean("isPro", false)
        set(value) { prefs.edit().putBoolean("isPro", value).apply() }
}
