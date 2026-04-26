package com.topit.ecotrace.data.remote

import com.topit.ecotrace.domain.model.Report

interface ReportsRemoteDataSource {
    suspend fun fetchReports(): List<Report>
    suspend fun createReport(report: Report): Report?
    suspend fun updateStatus(id: String, status: String): Boolean
    suspend fun deleteReport(id: String): Boolean
    suspend fun upsertReports(reports: List<Report>): Boolean
}
