package com.unknown.security.core.model

import kotlinx.serialization.Serializable

/** User-tunable behaviour of the interception engine; persisted as JSON. */
@Serializable
data class EnginePolicy(
    val autoActEnabled: Boolean = true,
    val autoActThreshold: ThreatSeverity = ThreatSeverity.HIGH,
    val dialogControlEnabled: Boolean = true,
    val freezeInsteadOfUninstall: Boolean = false,
    val protectSystemApps: Boolean = true,
    val emergencyEnabled: Boolean = true,
    val emergencyVolumePresses: Int = 3,
    val emergencyWindowMillis: Long = 1200L,
    val foregroundWatchEnabled: Boolean = true,
    val notificationsEnabled: Boolean = true,
    val overlayAlertsEnabled: Boolean = true,
) {
    val autoActScore: Int get() = autoActThreshold.score
}

/** A whitelisted package is never acted upon automatically. */
@Serializable
data class WhitelistEntry(
    val packageName: String,
    val addedMillis: Long,
    val note: String = "",
)

/** Live statistics shown on the dashboard. */
data class GuardStats(
    val watchedApps: Int = 0,
    val eventsToday: Int = 0,
    val totalThreats: Int = 0,
    val totalBlocked: Int = 0,
    val activeTierKey: String = GuardTier.STANDARD.key,
)
