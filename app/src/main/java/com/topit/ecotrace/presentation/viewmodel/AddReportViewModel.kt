package com.topit.ecotrace.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.usecase.AddReportUseCase
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddReportViewModel @Inject constructor(
    private val addReportUseCase: AddReportUseCase,
    private val authRepository: AuthRepository,
) : ViewModel() {
    fun createDraftReport(
        title: String,
        description: String,
        type: ProblemType,
        latitude: Double,
        longitude: Double,
        imageUri: String?,
    ) {
        viewModelScope.launch {
            addReportUseCase(
                Report(
                    title = title,
                    description = description,
                    type = type,
                    latitude = latitude,
                    longitude = longitude,
                    imageUri = imageUri,
                    authorId = authRepository.currentSession()?.userId ?: "local-user",
                ),
            )
        }
    }
}
