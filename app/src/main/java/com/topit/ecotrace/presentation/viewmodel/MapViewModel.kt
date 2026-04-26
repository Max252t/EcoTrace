package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportFilter
import com.topit.ecotrace.domain.usecase.GetReportsUseCase
import com.topit.ecotrace.domain.usecase.SyncReportsUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
class MapViewModel @Inject constructor(
    private val getReportsUseCase: GetReportsUseCase,
    private val syncReportsUseCase: SyncReportsUseCase,
) : ViewModel() {
    private val filterFlow = MutableStateFlow(ReportFilter())
    val filter: StateFlow<ReportFilter> = filterFlow

    val reports: StateFlow<List<Report>> = filterFlow
        .flatMapLatest { getReportsUseCase(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch {
            syncReportsUseCase()
        }
    }

    fun updateFilter(filter: ReportFilter) {
        filterFlow.update { filter }
    }
}
