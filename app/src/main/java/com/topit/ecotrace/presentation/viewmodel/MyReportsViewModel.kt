package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.usecase.DeleteReportUseCase
import com.topit.ecotrace.domain.usecase.GetReportsUseCase
import com.topit.ecotrace.domain.usecase.MarkReportResolvedUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

class MyReportsViewModel @Inject constructor(
    getReportsUseCase: GetReportsUseCase,
    private val markReportResolvedUseCase: MarkReportResolvedUseCase,
    private val deleteReportUseCase: DeleteReportUseCase,
) : ViewModel() {

    val reports: StateFlow<List<Report>> = getReportsUseCase(com.topit.ecotrace.domain.model.ReportFilter())
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun markResolved(reportId: String) {
        viewModelScope.launch { markReportResolvedUseCase(reportId) }
    }

    fun deleteReport(reportId: String) {
        viewModelScope.launch { deleteReportUseCase(reportId) }
    }
}
