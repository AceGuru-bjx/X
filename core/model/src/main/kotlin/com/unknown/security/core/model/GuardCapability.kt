package com.unknown.security.core.model

/**
 * A concrete interception capability. Tiers expose different sets and the
 * action router resolves the executable path for each capability.
 */
enum class GuardCapability(
    val label: String,
    val description: String,
) {
    INSTALL_MONITOR("安装监听", "应用安装 / 替换 / 卸载实时感知"),
    APP_INVENTORY("应用清单", "读取全部已安装应用并做风险体检"),
    FOREGROUND_WATCH("前台监控", "实时获知当前前台应用，风险应用上台即刻告警"),
    INSTALL_DIALOG_CONTROL("弹窗接管", "接管安装确认弹窗，风险应用自动点取消"),
    FORCE_STOP("强制停止", "无需用户确认直接结束风险应用进程"),
    SILENT_UNINSTALL("静默卸载", "静默移除风险应用"),
    HIDE_APP("隐藏应用", "从系统层面隐藏应用，防止再次启用"),
    FREEZE_APP("冻结应用", "停用（禁用）应用而不删除数据"),
    BLOCK_INSTALL("安装阻断", "系统级禁止新应用安装，装无可装"),
}
