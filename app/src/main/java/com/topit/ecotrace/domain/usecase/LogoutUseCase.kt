package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.repository.AchievementsRepository
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.ReportsRepository
import javax.inject.Inject

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val reportsRepository: ReportsRepository,
    private val achievementsRepository: AchievementsRepository,
) {
    suspend operator fun invoke() {
        authRepository.logout()
        reportsRepository.clearLocal()
        achievementsRepository.clearLocal()
    }
}
