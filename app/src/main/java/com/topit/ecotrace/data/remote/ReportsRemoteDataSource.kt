package com.topit.ecotrace.data.remote

import com.topit.ecotrace.domain.model.Report

enum class StatusUpdateResult { UPDATED, NOT_FOUND, FAILED }

interface ReportsRemoteDataSource {
    suspend fun fetchReports(): List<Report>
    suspend fun createReport(report: Report): Report?
    suspend fun updateStatus(id: String, status: String): StatusUpdateResult
    suspend fun deleteReport(id: String): Boolean
}
