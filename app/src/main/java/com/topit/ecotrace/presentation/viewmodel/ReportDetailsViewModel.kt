package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.usecase.GetAuthorNameUseCase
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
    private val getAuthorNameUseCase: GetAuthorNameUseCase,
) : ViewModel() {

    private val _report = MutableStateFlow<Report?>(null)
    val report: StateFlow<Report?> = _report.asStateFlow()

    private val _authorName = MutableStateFlow<String?>(null)
    val authorName: StateFlow<String?> = _authorName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun load(reportId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val report = getReportByIdUseCase(reportId)
            _report.value = report
            _isLoading.value = false
            _authorName.value = report?.let { getAuthorNameUseCase(it.authorId) }
        }
    }

    fun markResolved(reportId: String) {
        viewModelScope.launch {
            markReportResolvedUseCase(reportId)
            _report.value = getReportByIdUseCase(reportId)
        }
    }
}
