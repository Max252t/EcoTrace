package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.model.UserStats
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.usecase.GetAchievementsUseCase
import com.topit.ecotrace.domain.usecase.GetUserStatsUseCase
import com.topit.ecotrace.domain.usecase.SyncAchievementsUseCase
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val displayName: String = "",
    val stats: UserStats = UserStats(),
    val achievements: List<Achievement> = emptyList(),
)

class ProfileViewModel @Inject constructor(
    getUserStatsUseCase: GetUserStatsUseCase,
    getAchievementsUseCase: GetAchievementsUseCase,
    private val syncAchievementsUseCase: SyncAchievementsUseCase,
    authRepository: AuthRepository,
) : ViewModel() {

    val uiState: StateFlow<ProfileUiState> = combine(
        getUserStatsUseCase(),
        getAchievementsUseCase(),
    ) { stats, achievements ->
        ProfileUiState(
            displayName = authRepository.currentSession()?.displayName.orEmpty(),
            stats = stats,
            achievements = achievements,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ProfileUiState(
            displayName = authRepository.currentSession()?.displayName.orEmpty(),
        ),
    )

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch { syncAchievementsUseCase() }
    }
}
