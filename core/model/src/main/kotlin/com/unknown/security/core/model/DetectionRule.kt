package com.unknown.security.core.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class RuleSource(
    val label: String,
) {
    BUILTIN("内置"),
    USER("自定义"),
}

enum class RuleCategory(
    val label: String,
) {
    MALWARE("恶意软件"),
    ADWARE("广告插件"),
    RISKWARE("风险软件"),
    PRIVACY("隐私风险"),
    HEURISTIC("启发式规则"),
}

/**
 * A single detection rule. Rules are serializable so the whole rule set can be
 * exported, imported, shared and extended by the user.
 */
@Serializable
sealed class DetectionRule {
    abstract val id: String
    abstract val name: String
    abstract val description: String
    abstract val severity: ThreatSeverity
    abstract val category: RuleCategory
    abstract val enabled: Boolean
    abstract val source: RuleSource

    /** Exact package-name match (optionally regex). */
    @Serializable
    @SerialName("package_name")
    data class PackageNameRule(
        override val id: String,
        override val name: String,
        override val description: String,
        override val severity: ThreatSeverity,
        override val category: RuleCategory,
        override val enabled: Boolean = true,
        override val source: RuleSource = RuleSource.BUILTIN,
        val pattern: String,
        val isRegex: Boolean = false,
    ) : DetectionRule()

    /** App label contains any of the keywords. */
    @Serializable
    @SerialName("label_keyword")
    data class LabelKeywordRule(
        override val id: String,
        override val name: String,
        override val description: String,
        override val severity: ThreatSeverity,
        override val category: RuleCategory,
        override val enabled: Boolean = true,
        override val source: RuleSource = RuleSource.BUILTIN,
        val keywords: List<String>,
    ) : DetectionRule()

    /**
     * Requests all `required` permissions and at least `anyOfCount` of
     * `anyOf` (when provided). Classic heuristic for repackaged malware.
     */
    @Serializable
    @SerialName("permission_combo")
    data class PermissionComboRule(
        override val id: String,
        override val name: String,
        override val description: String,
        override val severity: ThreatSeverity,
        override val category: RuleCategory,
        override val enabled: Boolean = true,
        override val source: RuleSource = RuleSource.BUILTIN,
        val required: List<String>,
        val anyOf: List<String> = emptyList(),
        val anyOfCount: Int = 0,
    ) : DetectionRule()

    /** APK SHA-256 match — strongest possible static identification. */
    @Serializable
    @SerialName("apk_hash")
    data class ApkHashRule(
        override val id: String,
        override val name: String,
        override val description: String,
        override val severity: ThreatSeverity,
        override val category: RuleCategory,
        override val enabled: Boolean = true,
        override val source: RuleSource = RuleSource.BUILTIN,
        val sha256: String,
    ) : DetectionRule()

    /**
     * Target SDK far below the device while requesting dangerous permissions —
     * typical of abandoned or deliberately evasive packages.
     */
    @Serializable
    @SerialName("target_sdk")
    data class TargetSdkRule(
        override val id: String,
        override val name: String,
        override val description: String,
        override val severity: ThreatSeverity,
        override val category: RuleCategory,
        override val enabled: Boolean = true,
        override val source: RuleSource = RuleSource.BUILTIN,
        val maxTargetSdk: Int,
        val requireDangerousPermissions: Boolean = true,
    ) : DetectionRule()
}

/** A serializable collection of rules. */
@Serializable
data class RuleSet(
    val rules: List<DetectionRule> = emptyList(),
    val formatVersion: Int = 1,
    val generatedAtMillis: Long = 0L,
) {
    val enabledRules: List<DetectionRule> get() = rules.filter { it.enabled }

    companion object {
        val EMPTY = RuleSet()
    }
}
