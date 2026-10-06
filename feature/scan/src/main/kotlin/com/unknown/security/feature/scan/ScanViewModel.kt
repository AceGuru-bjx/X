package com.unknown.security.feature.scan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknown.security.core.model.AppRiskReport
import com.unknown.security.core.model.VerdictLevel
import com.unknown.security.data.repository.DisposalIntent
import com.unknown.security.data.repository.DisposalResult
import com.unknown.security.data.repository.GuardRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ScanViewModel(
    private val repository: GuardRepository,
) : ViewModel() {
    val reports = repository.scanReports
    val scanning = repository.scanning

    private val _busyPackage = MutableStateFlow<String?>(null)
    val busyPackage: StateFlow<String?> = _busyPackage.asStateFlow()

    private val _lastResult = MutableStateFlow<DisposalResult?>(null)
    val lastResult: StateFlow<DisposalResult?> = _lastResult.asStateFlow()

    private val _filter = MutableStateFlow(ScanFilter.ALL)
    val filter: StateFlow<ScanFilter> = _filter.asStateFlow()

    val filteredReports: StateFlow<List<AppRiskReport>> =
        combine(repository.scanReports, _filter) { list, currentFilter ->
            when (currentFilter) {
                ScanFilter.ALL -> list
                ScanFilter.DANGEROUS -> list.filter { it.verdict.level == VerdictLevel.DANGEROUS }
                ScanFilter.SUSPICIOUS -> list.filter { it.verdict.level == VerdictLevel.SUSPICIOUS }
                ScanFilter.CLEAN -> list.filter { it.verdict.level == VerdictLevel.CLEAN }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun scan() {
        viewModelScope.launch { repository.scanAll() }
    }

    fun setFilter(newFilter: ScanFilter) {
        _filter.value = newFilter
    }

    fun act(
        packageName: String,
        intent: DisposalIntent,
    ) {
        viewModelScope.launch {
            _busyPackage.value = packageName
            _lastResult.value = repository.actManually(packageName, intent)
            _busyPackage.value = null
        }
    }

    fun whitelist(packageName: String) {
        viewModelScope.launch {
            repository.addToWhitelist(packageName, "来自扫描页")
            repository.scanAll()
        }
    }

    fun clearResult() {
        _lastResult.value = null
    }
}

enum class ScanFilter(
    val label: String,
) {
    ALL("全部"),
    DANGEROUS("危险"),
    SUSPICIOUS("可疑"),
    CLEAN("安全"),
}
