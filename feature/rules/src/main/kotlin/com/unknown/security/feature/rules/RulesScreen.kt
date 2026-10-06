package com.unknown.security.feature.rules

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Upload
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
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
import com.unknown.security.core.designsystem.GlassModal
import com.unknown.security.core.designsystem.GlassScaffold
import com.unknown.security.core.designsystem.GlassTopBar
import com.unknown.security.core.designsystem.SeverityBadge
import com.unknown.security.core.model.DetectionRule
import com.unknown.security.core.model.RuleCategory
import com.unknown.security.core.model.RuleSource
import com.unknown.security.core.model.ThreatSeverity
import com.unknown.security.data.repository.GuardRepository

/** Rule & whitelist management: view, create, toggle, import/export. */
@Composable
fun RulesScreen(
    repository: GuardRepository,
    modifier: Modifier = Modifier,
) {
    val vm: RulesViewModel =
        viewModel(
            factory = viewModelFactory { initializer { RulesViewModel(repository) } },
        )
    val ruleSet by vm.ruleSet.collectAsState()
    val whitelist by vm.whitelist.collectAsState()
    val importMessage by vm.importMessage.collectAsState()
    val exportJson by vm.exportJson.collectAsState()
    var showCreate by remember { mutableStateOf(false) }
    var showWhitelist by remember { mutableStateOf(false) }
    var importDraft by remember { mutableStateOf("") }
    var showImport by remember { mutableStateOf(false) }

    GlassScaffold(
        modifier = modifier,
        topBar = { GlassTopBar(title = "规则中心", subtitle = "${ruleSet.rules.size} 条规则 · 白名单 ${whitelist.size}") },
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 140.dp, top = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(onClick = { showCreate = true }, emphasized = true, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("新建规则", fontWeight = FontWeight.Bold)
                }
                GlassButton(onClick = { showWhitelist = true }, modifier = Modifier.weight(1f)) {
                    Text("白名单 ${whitelist.size}")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                GlassButton(onClick = { vm.export() }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Upload, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("导出")
                }
                GlassButton(onClick = { showImport = true }, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.height(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("导入")
                }
            }

            if (ruleSet.rules.isEmpty()) {
                GlassCard(Modifier.fillMaxWidth()) {
                    Text(
                        text = "规则为空。导入 JSON 或手动创建。",
                        Modifier.padding(20.dp),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(ruleSet.rules, key = { it.id }) { rule ->
                        RuleRow(
                            rule = rule,
                            onToggle = { enabled -> vm.toggle(rule.id, enabled) },
                            onRemove = { vm.remove(rule.id) },
                        )
                    }
                }
            }
        }
    }

    if (showCreate) {
        CreateRuleModal(
            onDismiss = { showCreate = false },
            onCreate = { rule ->
                vm.upsert(rule)
                showCreate = false
            },
        )
    }

    if (showWhitelist) {
        GlassModal(
            visible = true,
            onDismissRequest = { showWhitelist = false },
            title = "白名单",
        ) {
            if (whitelist.isEmpty()) {
                Text(
                    "白名单为空。在扫描结果中可将应用加入白名单。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(whitelist, key = { it.packageName }) { entry ->
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = entry.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f),
                            )
                            GlassButton(onClick = { vm.removeFromWhitelist(entry.packageName) }) {
                                Text("移除")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showImport) {
        GlassModal(
            visible = true,
            onDismissRequest = { showImport = false },
            title = "导入规则 (JSON)",
            confirmLabel = "导入",
            onConfirm = {
                vm.import(importDraft)
                importDraft = ""
                showImport = false
            },
        ) {
            OutlinedTextField(
                value = importDraft,
                onValueChange = { importDraft = it },
                label = { Text("粘贴规则 JSON") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 4,
                colors = OutlinedTextFieldDefaults.colors(),
            )
        }
    }

    importMessage?.let { message ->
        GlassModal(
            visible = true,
            onDismissRequest = vm::dismissMessage,
            title = "导入结果",
        ) {
            Text(message, style = MaterialTheme.typography.bodyMedium)
        }
    }

    exportJson?.let { json ->
        GlassModal(
            visible = true,
            onDismissRequest = vm::dismissExport,
            title = "导出规则 (JSON)",
        ) {
            Text(
                text = json.take(4000),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun RuleRow(
    rule: DetectionRule,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    GlassCard(Modifier.fillMaxWidth(), cornerRadius = 18.dp) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = rule.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
                Switch(
                    checked = rule.enabled,
                    onCheckedChange = onToggle,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SeverityBadge(rule.severity)
                GlassChip(text = rule.category.label)
                GlassChip(text = rule.source.label)
            }
            Text(
                text = rule.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (rule.source == RuleSource.USER) {
                GlassButton(onClick = onRemove) { Text("删除") }
            }
        }
    }
}

@Composable
private fun CreateRuleModal(
    onDismiss: () -> Unit,
    onCreate: (DetectionRule) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf("") }
    var isRegex by remember { mutableStateOf(false) }
    var severity by remember { mutableStateOf(ThreatSeverity.HIGH) }

    GlassModal(
        visible = true,
        onDismissRequest = onDismiss,
        title = "新建包名规则",
        confirmLabel = "创建",
        onConfirm = {
            if (name.isNotBlank() && pattern.isNotBlank()) {
                onCreate(
                    DetectionRule.PackageNameRule(
                        id = "user-${System.currentTimeMillis()}",
                        name = name,
                        description = "用户自定义包名规则",
                        severity = severity,
                        category = RuleCategory.MALWARE,
                        enabled = true,
                        source = RuleSource.USER,
                        pattern = pattern,
                        isRegex = isRegex,
                    ),
                )
            }
        },
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.imePadding(),
        ) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("规则名称") },
                singleLine = true,
            )
            OutlinedTextField(
                value = pattern,
                onValueChange = { pattern = it },
                label = { Text("包名（精确或正则）") },
                singleLine = true,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "按正则匹配",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyMedium,
                )
                Switch(checked = isRegex, onCheckedChange = { isRegex = it })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThreatSeverity.entries.forEach { level ->
                    if (level == severity) {
                        GlassChip(text = level.label, tint = MaterialTheme.colorScheme.primary)
                    } else {
                        GlassChip(text = level.label)
                    }
                }
            }
        }
    }
}
