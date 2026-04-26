package com.topit.ecotrace.data.remote

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.mapper.toCreateRequest
import com.topit.ecotrace.data.mapper.toDomain
import com.topit.ecotrace.data.remote.api.ReportsApi
import com.topit.ecotrace.data.remote.api.UpdateStatusRequestDto
import com.topit.ecotrace.domain.model.Report
import javax.inject.Inject

class SupabaseReportsRemoteDataSource @Inject constructor(
    private val reportsApi: ReportsApi,
    private val sessionStorage: SessionStorage,
) : ReportsRemoteDataSource {
    override suspend fun fetchReports(): List<Report> {
        return runCatching { reportsApi.getReports().map { it.toDomain() } }.getOrDefault(emptyList())
    }

    override suspend fun createReport(report: Report): Report? {
        if (sessionStorage.token().isNullOrBlank()) return null
        return runCatching { reportsApi.createReport(report.toCreateRequest()).toDomain() }.getOrNull()
    }

    override suspend fun updateStatus(id: String, status: String): Boolean {
        if (sessionStorage.token().isNullOrBlank()) return false
        return runCatching {
            reportsApi.updateStatus(id, UpdateStatusRequestDto(status))
            true
        }.getOrDefault(false)
    }

    override suspend fun deleteReport(id: String): Boolean {
        if (sessionStorage.token().isNullOrBlank()) return false
        return runCatching { reportsApi.deleteReport(id).isSuccessful }.getOrDefault(false)
    }

    override suspend fun upsertReports(reports: List<Report>): Boolean {
        var allSynced = true
        reports.forEach { report ->
            val ok = if (report.status == com.topit.ecotrace.domain.model.ReportStatus.OPEN) {
                createReport(report) != null
            } else {
                updateStatus(report.id, report.status.name)
            }
            allSynced = allSynced && ok
        }
        return allSynced
    }
}
