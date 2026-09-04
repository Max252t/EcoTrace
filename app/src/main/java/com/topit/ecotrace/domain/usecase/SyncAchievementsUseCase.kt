package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.repository.AchievementsRepository
import javax.inject.Inject

class SyncAchievementsUseCase @Inject constructor(
    private val achievementsRepository: AchievementsRepository,
) {
    suspend operator fun invoke() {
        achievementsRepository.refresh()
    }
}
