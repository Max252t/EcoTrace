package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.repository.AchievementsRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetAchievementsUseCase @Inject constructor(
    private val achievementsRepository: AchievementsRepository,
) {
    operator fun invoke(): Flow<List<Achievement>> = achievementsRepository.observeAchievements()
}
