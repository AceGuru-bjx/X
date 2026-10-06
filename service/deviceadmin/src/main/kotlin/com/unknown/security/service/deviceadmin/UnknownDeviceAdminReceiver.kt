package com.unknown.security.service.deviceadmin

import android.app.admin.DeviceAdminReceiver
import android.content.Context
import android.content.Intent

/** Minimal device-admin receiver — the entry point for owner-level control. */
class UnknownDeviceAdminReceiver : DeviceAdminReceiver() {
    override fun onEnabled(
        context: Context,
        intent: Intent,
    ) = Unit

    override fun onDisabled(
        context: Context,
        intent: Intent,
    ) = Unit
}
