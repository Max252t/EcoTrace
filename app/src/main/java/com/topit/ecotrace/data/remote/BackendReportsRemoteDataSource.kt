package com.topit.ecotrace.data.remote

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.mapper.toCreateRequest
import com.topit.ecotrace.data.mapper.toDomain
import com.topit.ecotrace.data.remote.api.ReportsApi
import com.topit.ecotrace.data.remote.api.UpdateStatusRequestDto
import com.topit.ecotrace.domain.model.Report
import javax.inject.Inject
import retrofit2.HttpException

class BackendReportsRemoteDataSource @Inject constructor(
    private val reportsApi: ReportsApi,
    private val sessionStorage: SessionStorage,
    private val imageUploader: ImageUploader,
) : ReportsRemoteDataSource {
    override suspend fun fetchReports(): List<Report> {
        return runCatching { reportsApi.getReports().map { it.toDomain() } }.getOrDefault(emptyList())
    }

    override suspend fun createReport(report: Report): Report? {
        if (sessionStorage.token().isNullOrBlank()) return null
        val uploaded = withUploadedImage(report) ?: return null
        return runCatching { reportsApi.createReport(uploaded.toCreateRequest()).toDomain() }.getOrNull()
    }

    private suspend fun withUploadedImage(report: Report): Report? {
        val imageUri = report.imageUri ?: return report
        if (!imageUploader.isLocal(imageUri)) return report

        return when (val result = imageUploader.upload(imageUri)) {
            is ImageUploadResult.Success -> report.copy(imageUri = result.url)
            ImageUploadResult.Unavailable -> report.copy(imageUri = null)
            ImageUploadResult.Failed -> null
        }
    }

    override suspend fun updateStatus(id: String, status: String): StatusUpdateResult {
        if (sessionStorage.token().isNullOrBlank()) return StatusUpdateResult.FAILED
        return runCatching {
            reportsApi.updateStatus(id, UpdateStatusRequestDto(status))
            StatusUpdateResult.UPDATED
        }.getOrElse { error ->
            if (error is HttpException && error.code() == HTTP_NOT_FOUND) {
                StatusUpdateResult.NOT_FOUND
            } else {
                StatusUpdateResult.FAILED
            }
        }
    }

    override suspend fun deleteReport(id: String): Boolean {
        if (sessionStorage.token().isNullOrBlank()) return false
        return runCatching {
            val response = reportsApi.deleteReport(id)
            response.isSuccessful || response.code() == HTTP_NOT_FOUND
        }.getOrDefault(false)
    }

    private companion object {
        const val HTTP_NOT_FOUND = 404
    }
}
