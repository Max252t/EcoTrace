package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.usecase.GetAuthorNameUseCase
import com.topit.ecotrace.domain.usecase.GetReportByIdUseCase
import com.topit.ecotrace.domain.usecase.UpdateReportStatusUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

class ReportDetailsViewModel @Inject constructor(
    private val getReportByIdUseCase: GetReportByIdUseCase,
    private val updateReportStatusUseCase: UpdateReportStatusUseCase,
    private val getAuthorNameUseCase: GetAuthorNameUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _report = MutableStateFlow<Report?>(null)
    val report: StateFlow<Report?> = _report.asStateFlow()

    private val _authorName = MutableStateFlow<String?>(null)
    val authorName: StateFlow<String?> = _authorName.asStateFlow()

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _canChangeStatus = MutableStateFlow(false)
    val canChangeStatus: StateFlow<Boolean> = _canChangeStatus.asStateFlow()

    fun load(reportId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            val report = getReportByIdUseCase(reportId)
            _report.value = report
            _canChangeStatus.value = report != null && mayChangeStatus(report)
            _isLoading.value = false
            _authorName.value = report?.let { getAuthorNameUseCase(it.authorId) }
        }
    }

    fun updateStatus(reportId: String, status: ReportStatus) {
        viewModelScope.launch {
            updateReportStatusUseCase(reportId, status)
            _report.value = getReportByIdUseCase(reportId)
        }
    }

    private fun mayChangeStatus(report: Report): Boolean {
        val session = authRepository.currentSession() ?: return false
        return session.userId == report.authorId || session.role == ADMIN_ROLE
    }

    private companion object {
        const val ADMIN_ROLE = "ADMIN"
    }
}
