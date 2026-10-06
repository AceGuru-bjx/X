package com.unknown.security.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknown.security.core.model.GuardTier
import com.unknown.security.data.repository.GuardRepository
import com.unknown.security.data.repository.TierRequirementStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HomeViewModel(
    private val repository: GuardRepository,
) : ViewModel() {
    val stats = repository.stats
    val tier = repository.tier
    val guardEnabled = repository.guardEnabled
    val scanning = repository.scanning
    val lastScanMillis = repository.lastScanMillis

    private val _requirements = MutableStateFlow<List<TierRequirementStatus>>(emptyList())
    val requirements: StateFlow<List<TierRequirementStatus>> = _requirements.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    init {
        refreshRequirements()
    }

    fun refreshRequirements() {
        _requirements.value = repository.tierStatuses()
    }

    fun selectTier(newTier: GuardTier) {
        viewModelScope.launch {
            _busy.value = true
            repository.setTier(newTier)
            refreshRequirements()
            _busy.value = false
        }
    }

    fun setGuardEnabled(enabled: Boolean) {
        viewModelScope.launch { repository.setGuardEnabled(enabled) }
    }

    fun requirementOf(tier: GuardTier): TierRequirementStatus? = _requirements.value.firstOrNull { it.tier == tier }

    companion object {
        val AllTiers: List<GuardTier> = GuardTier.entries.toList()
    }
}
