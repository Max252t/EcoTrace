package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.usecase.DeleteReportUseCase
import com.topit.ecotrace.domain.usecase.GetMyReportsUseCase
import com.topit.ecotrace.domain.usecase.MarkReportResolvedUseCase
import com.topit.ecotrace.domain.usecase.SyncReportsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class MyReportsViewModel @Inject constructor(
    getMyReportsUseCase: GetMyReportsUseCase,
    private val syncReportsUseCase: SyncReportsUseCase,
    private val markReportResolvedUseCase: MarkReportResolvedUseCase,
    private val deleteReportUseCase: DeleteReportUseCase,
) : ViewModel() {

    val reports: StateFlow<List<Report>> = getMyReportsUseCase()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        viewModelScope.launch { syncReportsUseCase() }
    }

    fun markResolved(reportId: String) {
        viewModelScope.launch { markReportResolvedUseCase(reportId) }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch { deleteReportUseCase(reportId) }
    }
}
