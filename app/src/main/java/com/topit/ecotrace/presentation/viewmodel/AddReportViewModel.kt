package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.usecase.AddReportUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AddReportState { IDLE, SAVING, SAVED, SESSION_EXPIRED }

class AddReportViewModel @Inject constructor(
    private val addReportUseCase: AddReportUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(AddReportState.IDLE)
    val state: StateFlow<AddReportState> = _state.asStateFlow()

    fun createDraftReport(
        title: String,
        description: String,
        type: ProblemType,
        latitude: Double,
        longitude: Double,
        imageUri: String?,
    ) {
        if (_state.value == AddReportState.SAVING || _state.value == AddReportState.SAVED) return

        val authorId = authRepository.currentSession()?.userId
        if (authorId == null) {
            _state.value = AddReportState.SESSION_EXPIRED
            return
        }

        _state.value = AddReportState.SAVING
        viewModelScope.launch {
            addReportUseCase(
                Report(
                    title = title,
                    description = description,
                    type = type,
                    latitude = latitude,
                    longitude = longitude,
                    imageUri = imageUri,
                    authorId = authorId,
                ),
            )
            _state.value = AddReportState.SAVED
        }
    }
}
