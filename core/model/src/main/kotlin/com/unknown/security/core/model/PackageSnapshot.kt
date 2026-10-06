package com.unknown.security.core.model

import kotlinx.serialization.Serializable

/** Severity of a matched detection rule. Also carries the scoring weight. */
enum class ThreatSeverity(
    val score: Int,
    val label: String,
) {
    LOW(10, "低危"),
    MEDIUM(25, "中危"),
    HIGH(50, "高危"),
    CRITICAL(100, "严重"),
}

/** Overall verdict produced by the detection engine for one package. */
enum class VerdictLevel(
    val label: String,
) {
    CLEAN("安全"),
    SUSPICIOUS("可疑"),
    DANGEROUS("危险"),
}

/** Immutable snapshot of an installed package, taken by the inventory service. */
@Serializable
data class PackageSnapshot(
    val packageName: String,
    val label: String,
    val versionName: String,
    val versionCode: Long,
    val targetSdk: Int,
    val firstInstallMillis: Long,
    val lastUpdateMillis: Long,
    val installer: String,
    val permissions: List<String>,
    val isSystemApp: Boolean,
    val isUpdatedSystemApp: Boolean,
    val apkPath: String,
    val apkSizeBytes: Long,
    val sha256: String? = null,
) {
    val isPreInstalled: Boolean get() = isSystemApp && !isUpdatedSystemApp
}
