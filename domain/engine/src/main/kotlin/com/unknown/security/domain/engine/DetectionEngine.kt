package com.unknown.security.domain.engine

import com.unknown.security.core.model.DetectionRule
import com.unknown.security.core.model.MatchedRule
import com.unknown.security.core.model.PackageSnapshot
import com.unknown.security.core.model.RuleSet
import com.unknown.security.core.model.ScanVerdict
import com.unknown.security.core.model.ThreatSeverity
import com.unknown.security.core.model.VerdictLevel

/**
 * Pure-Kotlin detection engine: matches a package snapshot against a rule set
 * and produces a scored verdict. No Android dependencies — fully unit-testable.
 */
class DetectionEngine {
    /** Evaluates [snapshot] against [ruleSet], skipping packages in [whitelist]. */
    fun evaluate(
        snapshot: PackageSnapshot,
        ruleSet: RuleSet,
        whitelist: Set<String> = emptySet(),
    ): ScanVerdict {
        val matched = ruleSet.enabledRules.mapNotNull { rule -> match(rule, snapshot) }
        val score = matched.fold(0) { acc, m -> acc + m.severity.score }.coerceAtMost(MAX_SCORE)
        val level =
            when {
                matched.isEmpty() -> VerdictLevel.CLEAN
                score >= DANGEROUS_THRESHOLD -> VerdictLevel.DANGEROUS
                else -> VerdictLevel.SUSPICIOUS
            }
        return ScanVerdict(
            packageName = snapshot.packageName,
            level = level,
            score = score,
            matchedRules = matched,
            whitelisted = snapshot.packageName in whitelist,
        )
    }

    private fun match(
        rule: DetectionRule,
        snapshot: PackageSnapshot,
    ): MatchedRule? =
        when (rule) {
            is DetectionRule.PackageNameRule -> matchPackageName(rule, snapshot)
            is DetectionRule.LabelKeywordRule -> matchLabelKeyword(rule, snapshot)
            is DetectionRule.PermissionComboRule -> matchPermissionCombo(rule, snapshot)
            is DetectionRule.ApkHashRule -> matchApkHash(rule, snapshot)
            is DetectionRule.TargetSdkRule -> matchTargetSdk(rule, snapshot)
        }

    private fun matchPackageName(
        rule: DetectionRule.PackageNameRule,
        snapshot: PackageSnapshot,
    ): MatchedRule? {
        val hit =
            if (rule.isRegex) {
                runCatching { Regex(rule.pattern).matches(snapshot.packageName) }.getOrDefault(false)
            } else {
                snapshot.packageName == rule.pattern
            }
        return if (hit) {
            MatchedRule(rule.id, rule.name, rule.severity, "包名命中：${snapshot.packageName}")
        } else {
            null
        }
    }

    private fun matchLabelKeyword(
        rule: DetectionRule.LabelKeywordRule,
        snapshot: PackageSnapshot,
    ): MatchedRule? {
        val label = snapshot.label
        val keyword = rule.keywords.firstOrNull { label.contains(it, ignoreCase = true) }
        return if (keyword != null) {
            MatchedRule(rule.id, rule.name, rule.severity, "应用名命中关键词「$keyword」")
        } else {
            null
        }
    }

    private fun matchPermissionCombo(
        rule: DetectionRule.PermissionComboRule,
        snapshot: PackageSnapshot,
    ): MatchedRule? {
        val requested = snapshot.permissions.toSet()
        if (!rule.required.all { it in requested }) return null
        if (rule.anyOf.isNotEmpty() && rule.anyOfCount > 0) {
            val hits = rule.anyOf.count { it in requested }
            if (hits < rule.anyOfCount) return null
        }
        val requiredText = rule.required.joinToString(" + ") { it.substringAfterLast('.') }
        return MatchedRule(rule.id, rule.name, rule.severity, "权限组合命中：$requiredText")
    }

    private fun matchApkHash(
        rule: DetectionRule.ApkHashRule,
        snapshot: PackageSnapshot,
    ): MatchedRule? {
        val hash = snapshot.sha256 ?: return null
        return if (hash.equals(rule.sha256, ignoreCase = true)) {
            MatchedRule(rule.id, rule.name, rule.severity, "APK 哈希精确命中")
        } else {
            null
        }
    }

    private fun matchTargetSdk(
        rule: DetectionRule.TargetSdkRule,
        snapshot: PackageSnapshot,
    ): MatchedRule? {
        if (snapshot.targetSdk > rule.maxTargetSdk) return null
        if (rule.requireDangerousPermissions && snapshot.permissions.none { it in DangerousPermissions }) {
            return null
        }
        return MatchedRule(
            rule.id,
            rule.name,
            rule.severity,
            "targetSdk=${snapshot.targetSdk} 且申请敏感权限",
        )
    }

    companion object {
        const val MAX_SCORE = 100
        const val DANGEROUS_THRESHOLD = 50

        /** Canonical dangerous permissions used by the target-sdk heuristic. */
        val DangerousPermissions: Set<String> =
            setOf(
                "android.permission.READ_SMS",
                "android.permission.SEND_SMS",
                "android.permission.RECEIVE_SMS",
                "android.permission.READ_CONTACTS",
                "android.permission.WRITE_CONTACTS",
                "android.permission.CALL_PHONE",
                "android.permission.READ_CALL_LOG",
                "android.permission.WRITE_CALL_LOG",
                "android.permission.RECORD_AUDIO",
                "android.permission.CAMERA",
                "android.permission.ACCESS_FINE_LOCATION",
                "android.permission.ACCESS_COARSE_LOCATION",
                "android.permission.READ_PHONE_STATE",
                "android.permission.SYSTEM_ALERT_WINDOW",
                "android.permission.REQUEST_INSTALL_PACKAGES",
                "android.permission.BIND_ACCESSIBILITY_SERVICE",
                "android.permission.READ_EXTERNAL_STORAGE",
                "android.permission.WRITE_EXTERNAL_STORAGE",
            )
    }
}
