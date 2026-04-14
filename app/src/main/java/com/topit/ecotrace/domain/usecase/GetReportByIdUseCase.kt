package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.repository.ReportsRepository
import javax.inject.Inject

class GetReportByIdUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository,
) {
    suspend operator fun invoke(reportId: String): Report? =
        reportsRepository.getReportById(reportId)
}
