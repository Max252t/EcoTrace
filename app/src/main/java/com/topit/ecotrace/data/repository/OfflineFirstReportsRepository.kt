package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.ReportsDao
import com.topit.ecotrace.data.mapper.toDomain
import com.topit.ecotrace.data.mapper.toEntity
import com.topit.ecotrace.data.remote.ReportsRemoteDataSource
import com.topit.ecotrace.data.remote.StatusUpdateResult
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

    override suspend fun updateStatus(id: String, status: ReportStatus) {
        reportsDao.updateStatus(id, status.name)
        syncPending()
    }

    override suspend fun deleteReport(id: String) {
        reportsDao.markPendingDeletion(id)
        syncDeletions()
    }

    override suspend fun syncPending() {
        syncDeletions()

        val unsynced = reportsDao.getUnsyncedReports()
        val locallyChangedIds = reportsDao.getPendingDeletions().map { it.id }.toSet() +
            unsynced.map { it.id }.toSet()

        val remoteReports = remoteDataSource.fetchReports()
        if (remoteReports != null) {
            val fresh = remoteReports.filterNot { it.id in locallyChangedIds }
            if (fresh.isNotEmpty()) {
                reportsDao.insertAll(fresh.map { it.toEntity(synced = true, uploaded = true) })
            }

            val keptIds = remoteReports.map { it.id } + locallyChangedIds
            if (keptIds.isEmpty()) {
                reportsDao.deleteAllFromServer()
            } else {
                reportsDao.deleteMissingOnServer(keptIds)
            }
        }

        if (unsynced.isEmpty()) return

        unsynced.forEach { entity ->
            val report = entity.toDomain()
            val synced = if (entity.uploaded) {
                when (remoteDataSource.updateStatus(report.id, report.status.name)) {
                    StatusUpdateResult.UPDATED -> true
                    StatusUpdateResult.NOT_FOUND -> uploadReport(report)
                    StatusUpdateResult.FAILED -> false
                }
            } else {
                uploadReport(report)
            }

            if (synced) {
                reportsDao.markSynced(listOf(report.id))
            }
        }
    }

    override suspend fun clearLocal() {
        reportsDao.clear()
    }

    private suspend fun uploadReport(report: Report): Boolean {
        val created = remoteDataSource.createReport(report) ?: return false
        reportsDao.insert(created.toEntity(synced = true, uploaded = true))
        if (created.id != report.id) {
            reportsDao.deleteById(report.id)
        }
        return true
    }

    private suspend fun syncDeletions() {
        reportsDao.getPendingDeletions().forEach { entity ->
            if (remoteDataSource.deleteReport(entity.id)) {
                reportsDao.deleteById(entity.id)
            }
        }
    }
}
