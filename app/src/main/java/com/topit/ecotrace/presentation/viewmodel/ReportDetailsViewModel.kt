package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.usecase.GetReportByIdUseCase
import com.topit.ecotrace.domain.usecase.MarkReportResolvedUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ReportDetailsViewModel @Inject constructor(
    private val getReportByIdUseCase: GetReportByIdUseCase,
    private val markReportResolvedUseCase: MarkReportResolvedUseCase,
) : ViewModel() {

    private val _report = MutableStateFlow<Report?>(null)
    val report: StateFlow<Report?> = _report.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load(reportId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _report.value = getReportByIdUseCase(reportId)
            _isLoading.value = false
        }
    }

    fun markResolved(reportId: String) {
        viewModelScope.launch {
            markReportResolvedUseCase(reportId)
            _report.value = getReportByIdUseCase(reportId)
        }
    }
}
