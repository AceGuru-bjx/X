package com.unknown.security.feature.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknown.security.core.model.DetectionRule
import com.unknown.security.core.model.RuleSet
import com.unknown.security.core.model.WhitelistEntry
import com.unknown.security.data.repository.GuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class RulesViewModel(
    private val repository: GuardRepository,
) : ViewModel() {
    val ruleSet: StateFlow<RuleSet> =
        repository.rules.ruleSet
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), RuleSet.EMPTY)

    val whitelist: StateFlow<List<WhitelistEntry>> =
        repository.whitelistEntries
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    private val _importMessage = MutableStateFlow<String?>(null)
    val importMessage: StateFlow<String?> = _importMessage.asStateFlow()

    private val _exportJson = MutableStateFlow<String?>(null)
    val exportJson: StateFlow<String?> = _exportJson.asStateFlow()

    fun upsert(rule: DetectionRule) {
        viewModelScope.launch { repository.rules.upsertUserRule(rule) }
    }

    fun toggle(
        ruleId: String,
        enabled: Boolean,
    ) {
        viewModelScope.launch { repository.rules.toggleRule(ruleId, enabled) }
    }

    fun remove(ruleId: String) {
        viewModelScope.launch { repository.rules.removeRule(ruleId) }
    }

    fun removeFromWhitelist(packageName: String) {
        viewModelScope.launch { repository.removeFromWhitelist(packageName) }
    }

    fun import(text: String) {
        viewModelScope.launch {
            val result = repository.rules.importJson(text)
            _importMessage.value =
                result.fold(
                    onSuccess = { count -> "已导入 $count 条规则" },
                    onFailure = { "导入失败：${it.message}" },
                )
        }
    }

    fun export() {
        _exportJson.value = repository.rules.exportJson()
    }

    fun dismissExport() {
        _exportJson.value = null
    }

    fun dismissMessage() {
        _importMessage.value = null
    }
}
