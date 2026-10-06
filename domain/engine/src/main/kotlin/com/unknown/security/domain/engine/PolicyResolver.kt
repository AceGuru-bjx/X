package com.unknown.security.domain.engine

import com.unknown.security.core.model.EnginePolicy
import com.unknown.security.core.model.EventAction
import com.unknown.security.core.model.GuardCapability
import com.unknown.security.core.model.GuardTier
import com.unknown.security.core.model.ScanVerdict
import com.unknown.security.core.model.VerdictLevel

/** What the guard decided to do with a verdict, and why. */
data class ActionDecision(
    val action: EventAction,
    val reason: String,
    val usesCapability: GuardCapability? = null,
)

/**
 * Turns a scan verdict into a concrete action for the current tier and policy.
 * The decision is *what should happen*; the tier action router decides *how*
 * (via Shizuku shell, root shell or device policy).
 */
class PolicyResolver {
    fun resolve(
        verdict: ScanVerdict,
        policy: EnginePolicy,
        tier: GuardTier,
        isSystemApp: Boolean,
    ): ActionDecision {
        if (verdict.whitelisted) {
            return ActionDecision(EventAction.ALLOWED, "白名单应用，自动放行")
        }

        when (verdict.level) {
            VerdictLevel.CLEAN -> {
                return ActionDecision(EventAction.ALLOWED, "未命中规则")
            }

            VerdictLevel.SUSPICIOUS -> {
                return ActionDecision(EventAction.WARNED, "评分 ${verdict.score}：可疑，提示用户")
            }

            VerdictLevel.DANGEROUS -> {
                Unit
            }
        }

        if (!policy.autoActEnabled || verdict.score < policy.autoActScore) {
            return ActionDecision(EventAction.WARNED, "评分 ${verdict.score}：达到告警级别，等待用户处置")
        }

        if (isSystemApp && policy.protectSystemApps) {
            return ActionDecision(EventAction.WARNED, "系统应用受保护，仅提示不处置")
        }

        val prefersFreeze = policy.freezeInsteadOfUninstall
        return when {
            prefersFreeze && GuardCapability.FREEZE_APP in tier.capabilities -> {
                ActionDecision(EventAction.FROZEN, "冻结风险应用", GuardCapability.FREEZE_APP)
            }

            GuardCapability.SILENT_UNINSTALL in tier.capabilities -> {
                ActionDecision(EventAction.REMOVED, "静默卸载风险应用", GuardCapability.SILENT_UNINSTALL)
            }

            GuardCapability.FREEZE_APP in tier.capabilities -> {
                ActionDecision(EventAction.FROZEN, "冻结风险应用", GuardCapability.FREEZE_APP)
            }

            else -> {
                ActionDecision(EventAction.WARNED, "当前层级无自动处置能力，仅高优先级告警")
            }
        }
    }
}
