package com.unknown.security.core.model

import kotlinx.serialization.Serializable

enum class EventKind(
    val label: String,
) {
    INSTALL("新安装"),
    REPLACE("替换更新"),
    UNINSTALLED("已卸载"),
    FOREGROUND("前台风险"),
    INSTALL_DIALOG("安装弹窗拦截"),
    EMERGENCY("紧急触发"),
    MANUAL_SCAN("手动扫描"),
}

enum class EventAction(
    val label: String,
) {
    ALLOWED("放行"),
    WARNED("警示"),
    BLOCKED("已拦截"),
    DIALOG_CANCELED("弹窗已取消"),
    REMOVED("已卸载"),
    FROZEN("已冻结"),
    HIDDEN("已隐藏"),
    FORCE_STOPPED("已强停"),
    INSTALL_BLOCKED("安装被阻断"),
    NO_ACTION("无动作"),
}

/** A persisted interception event; the audit trail of the guard. */
@Serializable
data class InterceptionEvent(
    val id: Long,
    val timestampMillis: Long,
    val packageName: String,
    val appLabel: String,
    val kind: EventKind,
    val level: VerdictLevel,
    val score: Int,
    val matchedRuleNames: List<String> = emptyList(),
    val action: EventAction,
    val tierKey: String,
    val note: String = "",
)
