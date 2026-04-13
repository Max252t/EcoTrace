package com.topit.ecotrace.data.remote

import com.topit.ecotrace.domain.model.Report
import javax.inject.Inject

class SupabaseReportsRemoteDataSource @Inject constructor() : ReportsRemoteDataSource {
    override suspend fun upsertReports(reports: List<Report>): Boolean {
        // Placeholder for upcoming Supabase integration.
        return false
    }
}
