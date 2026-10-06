package com.unknown.security.service.accessibility

import android.content.ComponentName
import android.content.Context
import android.provider.Settings

/** Whether our accessibility service is enabled in system settings. */
object AccessibilityStatus {
    fun isServiceEnabled(context: Context): Boolean {
        val expected = ComponentName(context, UnknownAccessibilityService::class.java).flattenToString()
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (enabled.isNullOrBlank()) return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }
}
