package com.unknown.security.core.model

/**
 * The privilege tiers the guard can operate at. Every tier is a distinct
 * combination of Android privilege channels; higher tiers unlock stronger
 * interception capabilities. The tier system is intentionally composable:
 * accessibility adds real-time eyes, Shizuku adds adb-level hands,
 * root adds full machine control, device-owner adds enterprise-grade prevention.
 */
enum class GuardTier(
    val key: String,
    val displayLabel: String,
    val summary: String,
    val powerLevel: Int,
    val requiresAccessibility: Boolean,
    val requiresShizuku: Boolean,
    val requiresRoot: Boolean,
    val requiresDeviceOwner: Boolean,
    val capabilities: Set<GuardCapability>,
) {
    STANDARD(
        key = "standard",
        displayLabel = "标准模式",
        summary = "无需任何特殊权限：安装监听 + 全量应用风险扫描",
        powerLevel = 1,
        requiresAccessibility = false,
        requiresShizuku = false,
        requiresRoot = false,
        requiresDeviceOwner = false,
        capabilities =
            setOf(
                GuardCapability.INSTALL_MONITOR,
                GuardCapability.APP_INVENTORY,
            ),
    ),

    ACCESSIBLE(
        key = "accessible",
        displayLabel = "无障碍模式",
        summary = "实时前台监控 + 安装确认弹窗接管",
        powerLevel = 2,
        requiresAccessibility = true,
        requiresShizuku = false,
        requiresRoot = false,
        requiresDeviceOwner = false,
        capabilities =
            setOf(
                GuardCapability.INSTALL_MONITOR,
                GuardCapability.APP_INVENTORY,
                GuardCapability.FOREGROUND_WATCH,
                GuardCapability.INSTALL_DIALOG_CONTROL,
            ),
    ),

    SHIZUKU(
        key = "shizuku",
        displayLabel = "Shizuku 模式",
        summary = "以系统 shell 身份执行强停 / 卸载 / 冻结 / 隐藏",
        powerLevel = 3,
        requiresAccessibility = false,
        requiresShizuku = true,
        requiresRoot = false,
        requiresDeviceOwner = false,
        capabilities =
            setOf(
                GuardCapability.INSTALL_MONITOR,
                GuardCapability.APP_INVENTORY,
                GuardCapability.FORCE_STOP,
                GuardCapability.SILENT_UNINSTALL,
                GuardCapability.HIDE_APP,
                GuardCapability.FREEZE_APP,
            ),
    ),

    ACCESSIBLE_SHIZUKU(
        key = "accessible_shizuku",
        displayLabel = "无障碍 + Shizuku",
        summary = "实时监控 + 弹窗接管 + shell 级处置能力",
        powerLevel = 4,
        requiresAccessibility = true,
        requiresShizuku = true,
        requiresRoot = false,
        requiresDeviceOwner = false,
        capabilities =
            setOf(
                GuardCapability.INSTALL_MONITOR,
                GuardCapability.APP_INVENTORY,
                GuardCapability.FOREGROUND_WATCH,
                GuardCapability.INSTALL_DIALOG_CONTROL,
                GuardCapability.FORCE_STOP,
                GuardCapability.SILENT_UNINSTALL,
                GuardCapability.HIDE_APP,
                GuardCapability.FREEZE_APP,
            ),
    ),

    ROOT(
        key = "root",
        displayLabel = "Root 模式",
        summary = "以 root 身份执行一切系统操作",
        powerLevel = 4,
        requiresAccessibility = false,
        requiresShizuku = false,
        requiresRoot = true,
        requiresDeviceOwner = false,
        capabilities =
            setOf(
                GuardCapability.INSTALL_MONITOR,
                GuardCapability.APP_INVENTORY,
                GuardCapability.FORCE_STOP,
                GuardCapability.SILENT_UNINSTALL,
                GuardCapability.HIDE_APP,
                GuardCapability.FREEZE_APP,
            ),
    ),

    ACCESSIBLE_ROOT(
        key = "accessible_root",
        displayLabel = "无障碍 + Root",
        summary = "实时监控 + 弹窗接管 + root 级处置能力",
        powerLevel = 5,
        requiresAccessibility = true,
        requiresShizuku = false,
        requiresRoot = true,
        requiresDeviceOwner = false,
        capabilities =
            setOf(
                GuardCapability.INSTALL_MONITOR,
                GuardCapability.APP_INVENTORY,
                GuardCapability.FOREGROUND_WATCH,
                GuardCapability.INSTALL_DIALOG_CONTROL,
                GuardCapability.FORCE_STOP,
                GuardCapability.SILENT_UNINSTALL,
                GuardCapability.HIDE_APP,
                GuardCapability.FREEZE_APP,
            ),
    ),

    DEVICE_OWNER(
        key = "device_owner",
        displayLabel = "设备所有者模式",
        summary = "企业级管控：阻止安装 / 隐藏应用 / 禁止卸载",
        powerLevel = 4,
        requiresAccessibility = false,
        requiresShizuku = false,
        requiresRoot = false,
        requiresDeviceOwner = true,
        capabilities =
            setOf(
                GuardCapability.INSTALL_MONITOR,
                GuardCapability.APP_INVENTORY,
                GuardCapability.BLOCK_INSTALL,
                GuardCapability.HIDE_APP,
            ),
    ),

    ACCESSIBLE_DEVICE_OWNER(
        key = "accessible_device_owner",
        displayLabel = "无障碍 + 设备所有者",
        summary = "实时监控 + 企业级安装阻断与隐藏",
        powerLevel = 5,
        requiresAccessibility = true,
        requiresShizuku = false,
        requiresRoot = false,
        requiresDeviceOwner = true,
        capabilities =
            setOf(
                GuardCapability.INSTALL_MONITOR,
                GuardCapability.APP_INVENTORY,
                GuardCapability.FOREGROUND_WATCH,
                GuardCapability.INSTALL_DIALOG_CONTROL,
                GuardCapability.BLOCK_INSTALL,
                GuardCapability.HIDE_APP,
            ),
    ),

    ;

    val allCapabilityLabels: List<String>
        get() = capabilities.map { it.label }

    companion object {
        fun fromKey(key: String): GuardTier? = entries.firstOrNull { it.key == key }
    }
}
