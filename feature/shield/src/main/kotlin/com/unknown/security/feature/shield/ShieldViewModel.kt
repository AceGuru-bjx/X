package com.unknown.security.feature.shield

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.unknown.security.core.model.InterceptionEvent
import com.unknown.security.data.repository.GuardRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShieldViewModel(
    private val repository: GuardRepository,
) : ViewModel() {
    val events: StateFlow<List<InterceptionEvent>> =
        repository.eventLog
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val stats = repository.stats
    val foreground = repository.foreground

    fun clearLog() {
        viewModelScope.launch { repository.events.clear() }
    }

    fun refresh() {
        viewModelScope.launch { repository.events.loadRecent() }
    }
}
