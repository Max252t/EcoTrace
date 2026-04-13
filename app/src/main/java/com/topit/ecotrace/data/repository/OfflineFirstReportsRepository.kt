package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.ReportsDao
import com.topit.ecotrace.data.mapper.toDomain
import com.topit.ecotrace.data.mapper.toEntity
import com.topit.ecotrace.data.remote.ReportsRemoteDataSource
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class OfflineFirstReportsRepository @Inject constructor(
    private val reportsDao: ReportsDao,
    private val remoteDataSource: ReportsRemoteDataSource,
) : ReportsRepository {
    override fun observeReports(): Flow<List<Report>> {
        return reportsDao.observeReports().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun getReportById(id: String): Report? {
        return reportsDao.getById(id)?.toDomain()
    }

    override suspend fun createReport(report: Report) {
        reportsDao.insert(report.toEntity(synced = false))
    }

    override suspend fun updateReport(report: Report) {
        reportsDao.update(report.toEntity(synced = false))
    }

    override suspend fun markAsResolved(id: String) {
        reportsDao.updateStatus(id, ReportStatus.RESOLVED.name)
    }

    override suspend fun syncPending() {
        val unsynced = reportsDao.getUnsyncedReports()
        if (unsynced.isEmpty()) return

        val synced = remoteDataSource.upsertReports(unsynced.map { it.toDomain() })
        if (synced) {
            reportsDao.markSynced(unsynced.map { it.id })
        }
    }
}
