package com.topit.ecotrace.domain.repository

import com.topit.ecotrace.domain.model.Report
import kotlinx.coroutines.flow.Flow

interface ReportsRepository {
    fun observeReports(): Flow<List<Report>>
    suspend fun getReportById(id: String): Report?
    suspend fun createReport(report: Report)
    suspend fun updateReport(report: Report)
    suspend fun markAsResolved(id: String)
    suspend fun deleteReport(id: String)
    suspend fun syncPending()
}
