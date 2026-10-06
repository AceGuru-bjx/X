package com.unknown.security.feature.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unknown.security.core.designsystem.GlassButton
import com.unknown.security.core.designsystem.GlassCard
import com.unknown.security.core.designsystem.GlassChip
import com.unknown.security.core.designsystem.GlassScaffold
import com.unknown.security.core.designsystem.GlassTopBar
import com.unknown.security.core.model.EnginePolicy
import com.unknown.security.core.model.ThreatSeverity
import com.unknown.security.data.repository.GuardRepository
import com.unknown.security.service.accessibility.AccessibilityStatus
import com.unknown.security.service.deviceadmin.DeviceAdminState
import com.unknown.security.service.root.RootState
import com.unknown.security.service.shizuku.ShizukuState

/** Channel setup guides + engine policy knobs. */
@Composable
fun SettingsScreen(
    repository: GuardRepository,
    modifier: Modifier = Modifier,
) {
    val vm: SettingsViewModel =
        viewModel(
            factory = viewModelFactory { initializer { SettingsViewModel(repository) } },
        )
    val context = LocalContext.current
    val policy by vm.policy.collectAsState()
    val shizukuState by vm.shizukuState.collectAsState()
    val rootState by vm.rootState.collectAsState()
    val deviceAdminState by vm.deviceAdminState.collectAsState()
    val accessibilityOn = AccessibilityStatus.isServiceEnabled(context)

    DisposableEffect(Unit) {
        onDispose { }
    }

    GlassScaffold(
        modifier = modifier,
        topBar = { GlassTopBar(title = "设置", subtitle = "权限通道 · 引擎策略 · 关于") },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 140.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            SectionTitle("权限通道")

            ChannelCard(
                title = "无障碍服务",
                statusText = if (accessibilityOn) "已开启" else "未开启",
                ok = accessibilityOn,
                description = "实时前台监控、安装弹窗接管、音量键紧急触发的眼睛。",
                actionLabel = if (accessibilityOn) "查看系统设置" else "去系统设置开启",
                onAction = {
                    runCatching {
                        context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
                    }
                },
            )

            ChannelCard(
                title = "Shizuku (adb 特权)",
                statusText = shizukuState.displayLabel(),
                ok = shizukuState == ShizukuState.READY,
                description =
                    "以系统 shell 身份执行强停 / 卸载 / 冻结 / 隐藏，无需 root。" +
                        "需要安装并启动 Shizuku 应用（无线调试或 root 拉起）。",
                actionLabel =
                    when (shizukuState) {
                        ShizukuState.READY -> "已就绪"
                        ShizukuState.UNAUTHORIZED, ShizukuState.ASKING -> "请求授权"
                        else -> "查看 Shizuku"
                    },
                onAction = {
                    when (shizukuState) {
                        ShizukuState.UNAUTHORIZED, ShizukuState.ASKING -> {
                            vm.requestShizukuPermission()
                        }

                        else -> {
                            runCatching {
                                context.startActivity(
                                    Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app")).apply {
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    },
                                )
                            }
                        }
                    }
                },
            )

            ChannelCard(
                title = "Root 授权",
                statusText =
                    when (rootState) {
                        RootState.AVAILABLE -> "可用"
                        RootState.UNAVAILABLE -> "不可用"
                        RootState.UNCHECKED -> "未检测"
                    },
                ok = rootState == RootState.AVAILABLE,
                description = "通过 su 执行系统级处置。需要已 root 的设备并授予本应用授权。",
                actionLabel = "重新检测",
                onAction = { vm.probeRoot() },
            )

            ChannelCard(
                title = "设备所有者 (Device Owner)",
                statusText =
                    when (deviceAdminState) {
                        DeviceAdminState.DEVICE_OWNER -> "已配置"
                        DeviceAdminState.ACTIVE_ADMIN -> "仅管理员"
                        DeviceAdminState.NOT_PROVISIONED -> "未配置"
                    },
                ok = deviceAdminState == DeviceAdminState.DEVICE_OWNER,
                description =
                    "企业级管控：系统级禁止安装、隐藏应用。需要通过 adb 配置：" +
                        vm.provisioningCommand,
                actionLabel = "刷新状态",
                onAction = { vm.refresh() },
            )

            SectionTitle("引擎策略")

            PolicySwitchRow(
                title = "自动处置",
                subtitle = "达到阈值后自动卸载/冻结风险应用",
                checked = policy.autoActEnabled,
                onChange = { enabled -> vm.updatePolicy { it.copy(autoActEnabled = enabled) } },
            )

            Column {
                Text(
                    text = "自动处置阈值：${policy.autoActThreshold.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 6.dp)) {
                    ThreatSeverity.entries.forEach { severity ->
                        if (severity == policy.autoActThreshold) {
                            GlassChip(text = severity.label, tint = MaterialTheme.colorScheme.primary)
                        } else {
                            GlassChip(text = severity.label)
                        }
                    }
                }
            }

            PolicySwitchRow(
                title = "安装弹窗接管",
                subtitle = "安装确认弹窗出现时自动点取消（需无障碍）",
                checked = policy.dialogControlEnabled,
                onChange = { enabled -> vm.updatePolicy { it.copy(dialogControlEnabled = enabled) } },
            )

            PolicySwitchRow(
                title = "冻结优先",
                subtitle = "自动处置时优先冻结而非卸载（保留数据）",
                checked = policy.freezeInsteadOfUninstall,
                onChange = { enabled -> vm.updatePolicy { it.copy(freezeInsteadOfUninstall = enabled) } },
            )

            PolicySwitchRow(
                title = "保护系统应用",
                subtitle = "系统应用只提示不处置",
                checked = policy.protectSystemApps,
                onChange = { enabled -> vm.updatePolicy { it.copy(protectSystemApps = enabled) } },
            )

            PolicySwitchRow(
                title = "前台实时监控",
                subtitle = "危险应用进入前台立刻告警",
                checked = policy.foregroundWatchEnabled,
                onChange = { enabled -> vm.updatePolicy { it.copy(foregroundWatchEnabled = enabled) } },
            )

            PolicySwitchRow(
                title = "紧急触发",
                subtitle = "连按 ${policy.emergencyVolumePresses} 次音量键触发全盘体检与紧急处置",
                checked = policy.emergencyEnabled,
                onChange = { enabled -> vm.updatePolicy { it.copy(emergencyEnabled = enabled) } },
            )

            SectionTitle("关于")
            GlassCard(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Unknown 守护 · v1.0.0",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "多层权限拦截引擎 · 全局液态玻璃界面 · AGPL-3.0 开源",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text =
                            "界面由 Compose 原生构建，玻璃效果基于开源液态玻璃库（Apache-2.0）实现；" +
                                "特权通道依赖 Shizuku（Apache-2.0）。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

private fun ShizukuState.displayLabel(): String =
    when (this) {
        ShizukuState.NOT_INSTALLED -> "未安装"
        ShizukuState.NOT_RUNNING -> "未启动"
        ShizukuState.UNAUTHORIZED -> "未授权"
        ShizukuState.ASKING -> "授权中"
        ShizukuState.READY -> "已就绪"
    }

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@Composable
private fun ChannelCard(
    title: String,
    statusText: String,
    ok: Boolean,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                GlassChip(
                    text = statusText,
                    tint =
                        if (ok) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            com.unknown.security.core.designsystem.SeverityStyle
                                .severityColor(ThreatSeverity.MEDIUM)
                        },
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            GlassButton(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@Composable
private fun PolicySwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onChange: (Boolean) -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = onChange)
        }
    }
}
