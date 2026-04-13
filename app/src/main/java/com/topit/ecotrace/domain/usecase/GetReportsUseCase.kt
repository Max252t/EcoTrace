package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportFilter
import com.topit.ecotrace.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetReportsUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository,
) {
    operator fun invoke(filter: ReportFilter): Flow<List<Report>> {
        return reportsRepository.observeReports().map { reports ->
            reports.filter(filter::matches)
        }
    }
}
