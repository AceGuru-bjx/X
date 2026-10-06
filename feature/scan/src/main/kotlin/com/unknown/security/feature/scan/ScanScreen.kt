package com.unknown.security.feature.scan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BugReport
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.PowerSettingsNew
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.unknown.security.core.designsystem.GlassButton
import com.unknown.security.core.designsystem.GlassCard
import com.unknown.security.core.designsystem.GlassChip
import com.unknown.security.core.designsystem.GlassListItem
import com.unknown.security.core.designsystem.GlassModal
import com.unknown.security.core.designsystem.GlassScaffold
import com.unknown.security.core.designsystem.GlassTopBar
import com.unknown.security.core.designsystem.SeverityStyle
import com.unknown.security.core.designsystem.VerdictBadge
import com.unknown.security.core.model.AppRiskReport
import com.unknown.security.core.model.VerdictLevel
import com.unknown.security.data.repository.DisposalIntent
import com.unknown.security.data.repository.GuardRepository

/** Full-device scan: risk-sorted app list with per-app disposal actions. */
@Composable
fun ScanScreen(
    repository: GuardRepository,
    modifier: Modifier = Modifier,
) {
    val vm: ScanViewModel =
        viewModel(
            factory = viewModelFactory { initializer { ScanViewModel(repository) } },
        )
    val reports by vm.filteredReports.collectAsState()
    val allReports by vm.reports.collectAsState()
    val scanning by vm.scanning.collectAsState()
    val busyPackage by vm.busyPackage.collectAsState()
    val filter by vm.filter.collectAsState()
    val lastResult by vm.lastResult.collectAsState()
    var detail by remember { mutableStateOf<AppRiskReport?>(null) }

    Box(Modifier.fillMaxSize()) {
        GlassScaffold(
            modifier = modifier,
            topBar = { GlassTopBar(title = "全盘扫描", subtitle = summaryOf(allReports)) },
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 140.dp, top = 8.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ScanFilter.entries.forEach { currentFilter ->
                        FilterChip(currentFilter, filter == currentFilter, onSelect = vm::setFilter)
                    }
                }

                when {
                    scanning -> {
                        ScanningIndicator()
                    }

                    reports.isEmpty() && allReports.isEmpty() -> {
                        EmptyState(onScan = vm::scan)
                    }

                    else -> {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            items(reports, key = { it.snapshot.packageName }) { report ->
                                ReportRow(
                                    report = report,
                                    busy = busyPackage == report.snapshot.packageName,
                                    onClick = { detail = report },
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    detail?.let { report ->
        ReportDetailModal(
            report = report,
            onDismiss = { detail = null },
            onAct = { intent ->
                vm.act(report.snapshot.packageName, intent)
                detail = null
            },
            onWhitelist = {
                vm.whitelist(report.snapshot.packageName)
                detail = null
            },
        )
    }

    lastResult?.let { result ->
        GlassModal(
            visible = true,
            onDismissRequest = vm::clearResult,
            title = "处置结果",
            dismissLabel = "知道了",
            content = {
                Text(
                    text =
                        if (result.success) {
                            "操作成功 · ${result.detail}"
                        } else {
                            "操作失败 · ${result.detail}"
                        },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            },
        )
    }
}

@Composable
private fun FilterChip(
    filter: ScanFilter,
    selected: Boolean,
    onSelect: (ScanFilter) -> Unit,
) {
    GlassChip(
        text = if (selected) "${filter.label} ▾" else filter.label,
        tint =
            if (selected) {
                MaterialTheme.colorScheme.primary
            } else {
                androidx.compose.ui.graphics.Color.Unspecified
            },
        modifier = Modifier,
    )
}

@Composable
private fun ScanningIndicator() {
    GlassCard(Modifier.fillMaxWidth()) {
        Row(
            Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 2.5.dp,
            )
            Spacer(Modifier.width(14.dp))
            Text(
                text = "正在体检全部应用…",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun EmptyState(onScan: () -> Unit) {
    GlassCard(Modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Rounded.BugReport,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp),
            )
            Text(
                text = "还没有扫描结果",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            GlassButton(onClick = onScan, emphasized = true) {
                Text("立即开始全盘扫描", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun ReportRow(
    report: AppRiskReport,
    busy: Boolean,
    onClick: () -> Unit,
) {
    GlassListItem(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        leading = {
            if (busy) {
                CircularProgressIndicator(
                    modifier = Modifier.size(26.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Icon(
                    imageVector =
                        when (report.verdict.level) {
                            VerdictLevel.CLEAN -> Icons.Rounded.Verified
                            VerdictLevel.SUSPICIOUS -> Icons.Rounded.Layers
                            VerdictLevel.DANGEROUS -> Icons.Rounded.BugReport
                        },
                    contentDescription = null,
                    tint = SeverityStyle.verdictColor(report.verdict.level),
                    modifier = Modifier.size(26.dp),
                )
            }
        },
        headline = report.snapshot.label,
        supporting =
            report.snapshot.packageName +
                if (report.verdict.matchedRules.isNotEmpty()) " · 评分 ${report.verdict.score}" else "",
        trailing = {
            VerdictBadge(report.verdict.level)
        },
    )
}

@Composable
private fun ReportDetailModal(
    report: AppRiskReport,
    onDismiss: () -> Unit,
    onAct: (DisposalIntent) -> Unit,
    onWhitelist: () -> Unit,
) {
    GlassModal(
        visible = true,
        onDismissRequest = onDismiss,
        title = report.snapshot.label,
        content = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                VerdictBadge(report.verdict.level)
                DetailLine("包名", report.snapshot.packageName)
                DetailLine("版本", "${report.snapshot.versionName} (${report.snapshot.versionCode})")
                DetailLine("目标 SDK", report.snapshot.targetSdk.toString())
                DetailLine("安装来源", report.snapshot.installer)
                DetailLine("大小", formatSize(report.snapshot.apkSizeBytes))
                if (report.verdict.matchedRules.isNotEmpty()) {
                    Text(
                        text = "命中规则",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    report.verdict.matchedRules.forEach { matched ->
                        Text(
                            text = "· ${matched.ruleName}（${matched.severity.label}）${matched.detail}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (report.verdict.whitelisted) {
                    GlassChip(text = "已加入白名单，自动处置被跳过")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(
                        onClick = { onAct(DisposalIntent.UNINSTALL) },
                        emphasized = true,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.Block, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("卸载", fontWeight = FontWeight.Bold)
                    }
                    GlassButton(
                        onClick = { onAct(DisposalIntent.FORCE_STOP) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.PowerSettingsNew, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("强停")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GlassButton(
                        onClick = { onAct(DisposalIntent.FREEZE) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("冻结")
                    }
                    GlassButton(
                        onClick = { onAct(DisposalIntent.HIDE) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Rounded.VisibilityOff, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("隐藏")
                    }
                }
                GlassButton(onClick = onWhitelist, modifier = Modifier.fillMaxWidth()) {
                    Text("加入白名单")
                }
            }
        },
    )
}

@Composable
private fun DetailLine(
    label: String,
    value: String,
) {
    Row {
        Text(
            text = "$label：",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private fun summaryOf(reports: List<AppRiskReport>): String {
    if (reports.isEmpty()) return "尚未扫描"
    val dangerous = reports.count { it.verdict.level == VerdictLevel.DANGEROUS }
    val suspicious = reports.count { it.verdict.level == VerdictLevel.SUSPICIOUS }
    return "共 ${reports.size} 个应用 · 危险 $dangerous · 可疑 $suspicious"
}

private fun formatSize(bytes: Long): String =
    when {
        bytes >= 1024 * 1024 * 1024 -> "%.1f GB".format(bytes / 1024.0 / 1024.0 / 1024.0)
        bytes >= 1024 * 1024 -> "%.1f MB".format(bytes / 1024.0 / 1024.0)
        bytes >= 1024 -> "%.1f KB".format(bytes / 1024.0)
        else -> "$bytes B"
    }
