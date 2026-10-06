package com.unknown.security.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.GppGood
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.unknown.security.core.designsystem.SeverityStyle
import com.unknown.security.core.model.GuardTier
import com.unknown.security.data.repository.GuardRepository
import com.unknown.security.data.repository.TierRequirementStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Home dashboard: protection status, tier selector, quick stats & actions. */
@Composable
fun HomeScreen(
    repository: GuardRepository,
    onScan: () -> Unit,
    onShield: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val viewModel: HomeViewModel =
        viewModel(
            factory = viewModelFactory { initializer { HomeViewModel(repository) } },
        )
    val context = LocalContext.current
    val stats by viewModel.stats.collectAsState()
    val tier by viewModel.tier.collectAsState()
    val guardEnabled by viewModel.guardEnabled.collectAsState()
    val lastScan by viewModel.lastScanMillis.collectAsState()
    var setupTier by remember { mutableStateOf<GuardTier?>(null) }

    GlassScaffold(
        modifier = modifier,
        topBar = {
            GlassTopBar(
                title = "Unknown 守护",
                subtitle = if (guardEnabled) "守卫运行中 · ${tier.displayLabel}" else "守卫已暂停",
            )
        },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 140.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            StatusCard(
                tier = tier,
                enabled = guardEnabled,
                onToggle = viewModel::setGuardEnabled,
                stats = stats,
            )

            Text(
                text = "拦截层级",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )

            TierRow(
                selected = tier,
                onSelect = { selected ->
                    val requirement = viewModel.requirementOf(selected)
                    if (requirement?.satisfied == true) {
                        viewModel.selectTier(selected)
                    } else {
                        setupTier = selected
                    }
                },
            )

            QuickActions(
                lastScanMillis = lastScan,
                onScan = onScan,
                onShield = onShield,
            )
        }
    }

    setupTier?.let { tierForSetup ->
        val requirement = viewModel.requirementOf(tierForSetup)
        TierSetupModal(
            requirement = requirement,
            onDismiss = {
                setupTier = null
                viewModel.refreshRequirements()
            },
            onConfirm = {
                viewModel.selectTier(tierForSetup)
                setupTier = null
            },
        )
    }
}

@Composable
private fun StatusCard(
    tier: GuardTier,
    enabled: Boolean,
    onToggle: (Boolean) -> Unit,
    stats: com.unknown.security.core.model.GuardStats,
) {
    GlassCard(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (enabled) Icons.Rounded.HealthAndSafety else Icons.Rounded.GppGood,
                    contentDescription = null,
                    tint =
                        if (enabled) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    modifier = Modifier.size(34.dp),
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = if (enabled) "防护开启" else "防护暂停",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = tier.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = onToggle,
                    colors =
                        SwitchDefaults.colors(
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                        ),
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                repeat(5) { index ->
                    val active = index < tier.powerLevel
                    GlassChip(
                        text = "●",
                        tint =
                            if (active) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                Color(0x668A978F)
                            },
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "强度 ${tier.powerLevel}/5",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatCell("已盯防", stats.watchedApps.toString())
                StatCell("今日事件", stats.eventsToday.toString())
                StatCell("累计威胁", stats.totalThreats.toString())
                StatCell("已处置", stats.totalBlocked.toString())
            }
        }
    }
}

@Composable
private fun RowScope.StatCell(
    label: String,
    value: String,
) {
    GlassCard(
        modifier = Modifier.weight(1f),
        cornerRadius = 16.dp,
    ) {
        Column(
            Modifier.padding(vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun TierRow(
    selected: GuardTier,
    onSelect: (GuardTier) -> Unit,
) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(HomeViewModel.AllTiers, key = { it.key }) { tier ->
            TierCard(
                tier = tier,
                selected = tier == selected,
                onClick = { onSelect(tier) },
            )
        }
    }
}

@Composable
private fun TierCard(
    tier: GuardTier,
    selected: Boolean,
    onClick: () -> Unit,
) {
    GlassCard(
        modifier = Modifier.width(228.dp),
        cornerRadius = 20.dp,
        onClick = onClick,
        tint = if (selected) MaterialTheme.colorScheme.primary else Color.Unspecified,
    ) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = tier.displayLabel,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color =
                        if (selected) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                )
                Spacer(Modifier.weight(1f))
                GlassChip(text = "Lv.${tier.powerLevel}")
            }
            Text(
                text = tier.summary,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                tier.capabilities.take(4).forEach { capability ->
                    Text(
                        text = "· ${capability.label}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (tier.capabilities.size > 4) {
                    Text(
                        text = "… 共 ${tier.capabilities.size} 项能力",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickActions(
    lastScanMillis: Long,
    onScan: () -> Unit,
    onShield: () -> Unit,
) {
    val timeText =
        if (lastScanMillis > 0) {
            SimpleDateFormat("MM-dd HH:mm", Locale.getDefault()).format(Date(lastScanMillis))
        } else {
            "从未扫描"
        }
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        GlassButton(
            onClick = onScan,
            modifier = Modifier.weight(1f),
            emphasized = true,
        ) {
            Icon(Icons.Rounded.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(
                "全盘扫描",
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
            )
        }
        GlassButton(onClick = onShield, modifier = Modifier.weight(1f)) {
            Icon(Icons.Rounded.History, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text("拦截日志")
        }
    }
    Text(
        text = "上次扫描：$timeText · 连按音量键触发紧急体检",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
internal fun TierSetupModal(
    requirement: TierRequirementStatus?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    val tier = requirement?.tier ?: return
    com.unknown.security.core.designsystem.GlassModal(
        visible = true,
        onDismissRequest = onDismiss,
        title = "启用 ${tier.displayLabel}",
        confirmLabel = "仍然启用",
        onConfirm = onConfirm,
    ) {
        Text(
            text = tier.summary,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            RequirementRow("无障碍服务", requirement.accessibilityOk, tier.requiresAccessibility)
            RequirementRow("Shizuku 已启动并授权", requirement.shizukuOk, tier.requiresShizuku)
            RequirementRow("Root 授权可用", requirement.rootOk, tier.requiresRoot)
            RequirementRow("设备所有者已配置", requirement.deviceOwnerOk, tier.requiresDeviceOwner)
        }
        Text(
            text = "未满足的权限项可在「设置」中查看开通引导；未就绪时启用将只有部分能力生效。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RequirementRow(
    label: String,
    ok: Boolean,
    required: Boolean,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text =
                when {
                    !required -> "—"
                    ok -> "✓"
                    else -> "✕"
                },
            color =
                when {
                    !required -> MaterialTheme.colorScheme.onSurfaceVariant
                    ok -> MaterialTheme.colorScheme.primary
                    else -> SeverityStyle.severityColor(com.unknown.security.core.model.ThreatSeverity.HIGH)
                },
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}
