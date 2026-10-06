package com.unknown.security.app.guard

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the guard after reboot when it was running before. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        runCatching { GuardService.start(context) }
    }
}
