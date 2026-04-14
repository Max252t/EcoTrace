package com.ecotrace.backend.domain.repository

import com.ecotrace.backend.domain.model.ProblemType
import com.ecotrace.backend.domain.model.Report
import com.ecotrace.backend.domain.model.ReportStatus

interface ReportsRepository {
    suspend fun getAll(
        type: ProblemType? = null,
        status: ReportStatus? = null,
    ): List<Report>

    suspend fun getById(id: String): Report?
    suspend fun getByAuthor(authorId: String): List<Report>
    suspend fun create(report: Report): Report
    suspend fun updateStatus(id: String, status: ReportStatus): Report?
    suspend fun update(report: Report): Report?
    suspend fun delete(id: String): Boolean
}
