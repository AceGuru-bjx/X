package com.unknown.security.core.model

import kotlinx.serialization.Serializable

/** One rule that actually matched during evaluation. */
@Serializable
data class MatchedRule(
    val ruleId: String,
    val ruleName: String,
    val severity: ThreatSeverity,
    val detail: String,
)

/** Engine verdict for a single package. */
@Serializable
data class ScanVerdict(
    val packageName: String,
    val level: VerdictLevel,
    val score: Int,
    val matchedRules: List<MatchedRule> = emptyList(),
    val whitelisted: Boolean = false,
) {
    companion object {
        fun clean(packageName: String) =
            ScanVerdict(
                packageName = packageName,
                level = VerdictLevel.CLEAN,
                score = 0,
            )
    }
}

/** Verdict + the snapshot it was computed from, used by scan UI. */
data class AppRiskReport(
    val snapshot: PackageSnapshot,
    val verdict: ScanVerdict,
)
