package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.repository.ReportsRepository
import javax.inject.Inject

class SyncReportsUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository,
) {
    suspend operator fun invoke() {
        reportsRepository.syncPending()
    }
}
