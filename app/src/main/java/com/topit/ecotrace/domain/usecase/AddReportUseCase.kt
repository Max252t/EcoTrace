package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.repository.ReportsRepository
import javax.inject.Inject

class AddReportUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository,
) {
    suspend operator fun invoke(report: Report) {
        reportsRepository.createReport(report)
    }
}
