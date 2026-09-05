package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.repository.ReportsRepository
import javax.inject.Inject

class UpdateReportStatusUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository,
) {
    suspend operator fun invoke(reportId: String, status: ReportStatus) {
        reportsRepository.updateStatus(reportId, status)
    }
}
