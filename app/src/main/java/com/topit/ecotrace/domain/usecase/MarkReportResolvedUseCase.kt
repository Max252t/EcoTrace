package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.repository.ReportsRepository
import javax.inject.Inject

class MarkReportResolvedUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository,
) {
    suspend operator fun invoke(reportId: String) {
        reportsRepository.markAsResolved(reportId)
    }
}
