package com.topit.ecotrace.data.repository

import com.topit.ecotrace.data.local.ReportEntity
import com.topit.ecotrace.data.local.ReportsDao
import com.topit.ecotrace.data.mapper.toEntity
import com.topit.ecotrace.data.remote.ReportsRemoteDataSource
import com.topit.ecotrace.data.remote.StatusUpdateResult
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class OfflineFirstReportsRepositoryTest {

    @Test
    fun deleteReport_removesReportFromUiImmediatelyWhileOffline() = runBlocking {
        val dao = FakeReportsDao(listOf(entity("r-1")))
        val remote = FakeReportsRemoteDataSource(offline = true)
        val repository = repository(dao, remote)

        repository.deleteReport("r-1")

        assertEquals(emptyList<Report>(), repository.observeReports().first())
        assertNull(repository.getReportById("r-1"))
    }

    @Test
    fun deleteReport_queuesDeletionWhenServerIsUnreachable() = runBlocking {
        val dao = FakeReportsDao(listOf(entity("r-1")))
        val remote = FakeReportsRemoteDataSource(offline = true)
        val repository = repository(dao, remote)

        repository.deleteReport("r-1")

        assertEquals(listOf("r-1"), dao.getPendingDeletions().map { it.id })
        assertEquals(emptyList<String>(), remote.deletedIds)
    }

    @Test
    fun deleteReport_removesReportLocallyWhenServerAcceptsDeletion() = runBlocking {
        val dao = FakeReportsDao(listOf(entity("r-1")))
        val remote = FakeReportsRemoteDataSource()
        val repository = repository(dao, remote)

        repository.deleteReport("r-1")

        assertEquals(listOf("r-1"), remote.deletedIds)
        assertEquals(emptyList<ReportEntity>(), dao.rows())
    }

    @Test
    fun syncPending_doesNotRestoreReportQueuedForDeletion() = runBlocking {
        val report = report("r-1")
        val dao = FakeReportsDao(listOf(entity("r-1")))
        val remote = FakeReportsRemoteDataSource(offline = true, serverReports = listOf(report))
        val repository = repository(dao, remote)

        repository.deleteReport("r-1")
        remote.offlineForDelete = true
        remote.offline = false
        repository.syncPending()

        assertEquals(emptyList<Report>(), repository.observeReports().first())
        assertEquals(listOf("r-1"), dao.getPendingDeletions().map { it.id })
    }

    @Test
    fun syncPending_pushesQueuedDeletionOnceServerIsReachable() = runBlocking {
        val dao = FakeReportsDao(listOf(entity("r-1")))
        val remote = FakeReportsRemoteDataSource(offline = true)
        val repository = repository(dao, remote)

        repository.deleteReport("r-1")
        remote.offline = false
        repository.syncPending()

        assertEquals(listOf("r-1"), remote.deletedIds)
        assertEquals(emptyList<ReportEntity>(), dao.rows())
        assertEquals(emptyList<ReportEntity>(), dao.getPendingDeletions())
    }

    @Test
    fun syncPending_keepsDeletionQueuedWhenServerRejectsIt() = runBlocking {
        val dao = FakeReportsDao(listOf(entity("r-1")))
        val remote = FakeReportsRemoteDataSource(deletionAllowed = false)
        val repository = repository(dao, remote)

        repository.deleteReport("r-1")
        repository.syncPending()

        assertEquals(listOf("r-1"), dao.getPendingDeletions().map { it.id })
        assertEquals(emptyList<Report>(), repository.observeReports().first())
    }

    @Test
    fun deletedReport_isNotOfferedForUpload() = runBlocking {
        val dao = FakeReportsDao(listOf(entity("r-1", synced = false)))
        val remote = FakeReportsRemoteDataSource(offline = true)
        val repository = repository(dao, remote)

        repository.deleteReport("r-1")
        remote.offline = false
        remote.offlineForDelete = true
        repository.syncPending()

        assertEquals(emptyList<Report>(), remote.createdReports)
    }

    @Test
    fun updateStatus_storesStatusLocallyWhileOffline() = runBlocking {
        val dao = FakeReportsDao(listOf(entity("r-1")))
        val remote = FakeReportsRemoteDataSource(offline = true)
        val repository = repository(dao, remote)

        repository.updateStatus("r-1", ReportStatus.IN_PROGRESS)

        val stored = dao.rows().single()
        assertEquals(ReportStatus.IN_PROGRESS.name, stored.status)
        assertFalse(stored.synced)
        assertEquals(emptyList<Pair<String, String>>(), remote.statusUpdates)
    }

    @Test
    fun updateStatus_pushesInProgressToServerWhenOnline() = runBlocking {
        val report = report("r-1")
        val dao = FakeReportsDao(listOf(entity("r-1")))
        val remote = FakeReportsRemoteDataSource(serverReports = listOf(report))
        val repository = repository(dao, remote)

        repository.updateStatus("r-1", ReportStatus.IN_PROGRESS)

        assertEquals(listOf("r-1" to ReportStatus.IN_PROGRESS.name), remote.statusUpdates)
        assertEquals(ReportStatus.IN_PROGRESS.name, dao.rows().single().status)
        assertTrue(dao.rows().single().synced)
    }

    @Test
    fun syncPending_uploadsReportThatServerDoesNotKnowYet() = runBlocking {
        val dao = FakeReportsDao(
            listOf(entity("r-1", synced = false, status = ReportStatus.IN_PROGRESS)),
        )
        val remote = FakeReportsRemoteDataSource()
        val repository = repository(dao, remote)

        repository.syncPending()

        assertEquals(listOf("r-1"), remote.createdReports.map { it.id })
        assertTrue(dao.rows().single().synced)
    }

    @Test
    fun createReport_storesReportLocallyAsUnsynced() = runBlocking {
        val dao = FakeReportsDao()
        val remote = FakeReportsRemoteDataSource(offline = true)
        val repository = repository(dao, remote)

        repository.createReport(report("r-1"))

        val stored = dao.rows().single()
        assertEquals("r-1", stored.id)
        assertFalse(stored.synced)
        assertFalse(stored.pendingDeletion)
    }

    @Test
    fun syncPending_uploadsLocallyCreatedReport() = runBlocking {
        val dao = FakeReportsDao()
        val remote = FakeReportsRemoteDataSource(offline = true)
        val repository = repository(dao, remote)

        repository.createReport(report("r-1"))
        remote.offline = false
        repository.syncPending()

        assertEquals(listOf("r-1"), remote.createdReports.map { it.id })
        assertTrue(dao.rows().single().synced)
    }

    private fun repository(dao: ReportsDao, remote: ReportsRemoteDataSource) =
        OfflineFirstReportsRepository(reportsDao = dao, remoteDataSource = remote)

    private fun report(
        id: String,
        status: ReportStatus = ReportStatus.OPEN,
    ) = Report(
        id = id,
        title = "title",
        description = "description",
        type = ProblemType.DUMP,
        status = status,
        latitude = 55.0,
        longitude = 37.0,
        authorId = "user-1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        synced = false,
    )

    private fun entity(
        id: String,
        synced: Boolean = true,
        status: ReportStatus = ReportStatus.OPEN,
    ) = report(id, status).toEntity(synced = synced)
}

private class FakeReportsDao(initial: List<ReportEntity> = emptyList()) : ReportsDao {
    private val stored = MutableStateFlow(initial)

    fun rows(): List<ReportEntity> = stored.value

    override fun observeReports(): Flow<List<ReportEntity>> =
        stored.map { rows -> rows.filterNot { it.pendingDeletion } }

    override suspend fun getById(id: String): ReportEntity? =
        stored.value.firstOrNull { it.id == id && !it.pendingDeletion }

    override suspend fun insert(report: ReportEntity) = insertAll(listOf(report))

    override suspend fun insertAll(reports: List<ReportEntity>) {
        val updated = stored.value.toMutableList()
        reports.forEach { report ->
            updated.removeAll { it.id == report.id }
            updated += report
        }
        stored.value = updated
    }

    override suspend fun update(report: ReportEntity) = insertAll(listOf(report))

    override suspend fun updateStatus(id: String, status: String) {
        stored.value = stored.value.map {
            if (it.id == id) it.copy(status = status, synced = false) else it
        }
    }

    override suspend fun getUnsyncedReports(): List<ReportEntity> =
        stored.value.filter { !it.synced && !it.pendingDeletion }

    override suspend fun markSynced(ids: List<String>) {
        stored.value = stored.value.map { if (it.id in ids) it.copy(synced = true) else it }
    }

    override suspend fun markPendingDeletion(id: String) {
        stored.value = stored.value.map {
            if (it.id == id) it.copy(pendingDeletion = true, synced = false) else it
        }
    }

    override suspend fun getPendingDeletions(): List<ReportEntity> =
        stored.value.filter { it.pendingDeletion }

    override suspend fun deleteById(id: String) {
        stored.value = stored.value.filterNot { it.id == id }
    }
}

private class FakeReportsRemoteDataSource(
    var offline: Boolean = false,
    var offlineForDelete: Boolean = false,
    private val deletionAllowed: Boolean = true,
    private val serverReports: List<Report> = emptyList(),
) : ReportsRemoteDataSource {

    var deletedIds: List<String> = emptyList()
        private set

    var createdReports: List<Report> = emptyList()
        private set

    var statusUpdates: List<Pair<String, String>> = emptyList()
        private set

    private val knownIds: MutableSet<String> = serverReports.map { it.id }.toMutableSet()

    override suspend fun fetchReports(): List<Report> = if (offline) emptyList() else serverReports

    override suspend fun createReport(report: Report): Report? {
        if (offline) return null
        createdReports = createdReports + report
        knownIds += report.id
        return report.copy(synced = true)
    }

    override suspend fun updateStatus(id: String, status: String): StatusUpdateResult {
        if (offline) return StatusUpdateResult.FAILED
        if (id !in knownIds) return StatusUpdateResult.NOT_FOUND
        statusUpdates = statusUpdates + (id to status)
        return StatusUpdateResult.UPDATED
    }

    override suspend fun deleteReport(id: String): Boolean {
        if (offline || offlineForDelete || !deletionAllowed) return false
        deletedIds = deletedIds + id
        return true
    }

}
