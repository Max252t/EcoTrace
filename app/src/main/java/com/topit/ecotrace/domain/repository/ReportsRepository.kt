package com.topit.ecotrace.domain.repository

import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import kotlinx.coroutines.flow.Flow

interface ReportsRepository {
    fun observeReports(): Flow<List<Report>>
    suspend fun getReportById(id: String): Report?
    suspend fun createReport(report: Report)
    suspend fun updateStatus(id: String, status: ReportStatus)
    suspend fun deleteReport(id: String)
    suspend fun syncPending()
}
