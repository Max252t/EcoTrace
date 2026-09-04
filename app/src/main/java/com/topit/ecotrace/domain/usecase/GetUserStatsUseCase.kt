package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.model.AchievementRules
import com.topit.ecotrace.domain.model.UserStats
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetUserStatsUseCase @Inject constructor(
    private val getMyReportsUseCase: GetMyReportsUseCase,
) {
    operator fun invoke(): Flow<UserStats> = getMyReportsUseCase().map(AchievementRules::statsOf)
}
