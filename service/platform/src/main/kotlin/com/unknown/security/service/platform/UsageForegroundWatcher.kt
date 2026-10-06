package com.unknown.security.service.platform

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Foreground watcher built on UsageStatsManager — works without accessibility,
 * at the cost of polling (used as fallback / baseline observation).
 */
class UsageForegroundWatcher(
    private val context: Context,
    private val scope: CoroutineScope,
) {
    private val _foregroundPackage = MutableStateFlow<String?>(null)
    val foregroundPackage: StateFlow<String?> = _foregroundPackage.asStateFlow()

    private var job: Job? = null

    /** Whether the user granted "Usage access" in system settings. */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        val mode =
            runCatching {
                appOps.unsafeCheckOpNoThrow(
                    AppOpsManager.OPSTR_GET_USAGE_STATS,
                    Process.myUid(),
                    context.packageName,
                )
            }.getOrDefault(AppOpsManager.MODE_ERRORED)
        return mode == AppOpsManager.MODE_ALLOWED
    }

    fun start(pollIntervalMillis: Long = DEFAULT_INTERVAL) {
        stop()
        job =
            scope.launch {
                while (isActive) {
                    tick()
                    delay(pollIntervalMillis)
                }
            }
    }

    fun stop() {
        job?.cancel()
        job = null
    }

    private fun tick() {
        val usage = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
        val now = System.currentTimeMillis()
        val begin = now - POLL_WINDOW
        val events = usage.queryEvents(begin, now) ?: return
        var latestPackage: String? = null
        var latestTime = 0L
        val event = UsageEvents.Event()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                if (event.timeStamp >= latestTime) {
                    latestTime = event.timeStamp
                    latestPackage = event.packageName
                }
            }
        }
        if (latestPackage != null && latestPackage != _foregroundPackage.value) {
            _foregroundPackage.value = latestPackage
        }
    }

    private companion object {
        const val DEFAULT_INTERVAL = 2500L
        const val POLL_WINDOW = 10_000L
    }
}
