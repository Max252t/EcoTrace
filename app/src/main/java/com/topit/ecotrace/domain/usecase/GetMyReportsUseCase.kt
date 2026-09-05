package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.ReportsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetMyReportsUseCase @Inject constructor(
    private val reportsRepository: ReportsRepository,
    private val authRepository: AuthRepository,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<List<Report>> {
        return authRepository.observeSession().flatMapLatest { session ->
            val userId = session?.userId ?: return@flatMapLatest flowOf(emptyList())
            reportsRepository.observeReports().map { reports ->
                reports.filter { it.authorId == userId }
            }
        }
    }
}
