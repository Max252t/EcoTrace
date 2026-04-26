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
        syncPending()
    }

    override suspend fun updateReport(report: Report) {
        reportsDao.update(report.toEntity(synced = false))
        syncPending()
    }

    override suspend fun markAsResolved(id: String) {
        reportsDao.updateStatus(id, ReportStatus.RESOLVED.name)
        syncPending()
    }

    override suspend fun deleteReport(id: String) {
        reportsDao.deleteById(id)
        remoteDataSource.deleteReport(id)
    }

    override suspend fun syncPending() {
        val remoteReports = remoteDataSource.fetchReports()
        if (remoteReports.isNotEmpty()) {
            reportsDao.insertAll(remoteReports.map { it.toEntity(synced = true) })
        }

        val unsynced = reportsDao.getUnsyncedReports()
        if (unsynced.isEmpty()) return

        unsynced.forEach { entity ->
            val report = entity.toDomain()
            val synced = if (report.status == ReportStatus.OPEN) {
                val created = remoteDataSource.createReport(report)
                if (created != null) {
                    reportsDao.insert(created.toEntity(synced = true))
                    if (created.id != report.id) {
                        reportsDao.deleteById(report.id)
                    } else {
                        reportsDao.markSynced(listOf(report.id))
                    }
                    true
                } else {
                    false
                }
            } else {
                remoteDataSource.updateStatus(report.id, report.status.name)
            }

            if (synced) {
                reportsDao.markSynced(listOf(report.id))
            }
        }
    }
}
