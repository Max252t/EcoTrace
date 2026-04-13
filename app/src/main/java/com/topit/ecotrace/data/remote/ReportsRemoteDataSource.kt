package com.topit.ecotrace.data.remote

import com.topit.ecotrace.domain.model.Report

interface ReportsRemoteDataSource {
    suspend fun upsertReports(reports: List<Report>): Boolean
}
