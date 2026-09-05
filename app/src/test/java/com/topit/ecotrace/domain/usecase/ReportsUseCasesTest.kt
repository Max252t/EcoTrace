package com.topit.ecotrace.domain.usecase

import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportFilter
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import com.topit.ecotrace.domain.repository.ReportsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class ReportsUseCasesTest {

    @Test
    fun addReportUseCase_delegatesCreateToRepository() = runBlocking {
        val repository = FakeReportsRepository()
        val useCase = AddReportUseCase(repository)
        val report = testReport(id = "r-1")

        useCase(report)

        assertEquals(report, repository.createdReport)
    }

    @Test
    fun deleteReportUseCase_delegatesDeleteToRepository() = runBlocking {
        val repository = FakeReportsRepository()
        val useCase = DeleteReportUseCase(repository)

        useCase("r-2")

        assertEquals("r-2", repository.deletedReportId)
    }

    @Test
    fun updateReportStatusUseCase_delegatesStatusChangeToRepository() = runBlocking {
        val repository = FakeReportsRepository()
        val useCase = UpdateReportStatusUseCase(repository)

        useCase("r-3", ReportStatus.IN_PROGRESS)

        assertEquals("r-3" to ReportStatus.IN_PROGRESS, repository.statusChange)
    }

    @Test
    fun syncReportsUseCase_callsSyncPending() = runBlocking {
        val repository = FakeReportsRepository()
        val useCase = SyncReportsUseCase(repository)

        useCase()

        assertEquals(1, repository.syncPendingCalls)
    }

    @Test
    fun getReportByIdUseCase_returnsValueFromRepository() = runBlocking {
        val report = testReport(id = "r-4")
        val repository = FakeReportsRepository(reportById = mapOf(report.id to report))
        val useCase = GetReportByIdUseCase(repository)

        val result = useCase("r-4")

        assertEquals(report, result)
    }

    @Test
    fun getReportByIdUseCase_returnsNullWhenRepositoryHasNoReport() = runBlocking {
        val repository = FakeReportsRepository()
        val useCase = GetReportByIdUseCase(repository)

        val result = useCase("missing")

        assertNull(result)
    }

    @Test
    fun getReportsUseCase_appliesFilterToObservedReports() = runBlocking {
        val openDump = testReport(id = "1", type = ProblemType.DUMP, status = ReportStatus.OPEN)
        val resolvedDump = testReport(id = "2", type = ProblemType.DUMP, status = ReportStatus.RESOLVED)
        val openTree = testReport(id = "3", type = ProblemType.FALLEN_TREE, status = ReportStatus.OPEN)
        val repository = FakeReportsRepository(
            observedReports = listOf(openDump, resolvedDump, openTree),
        )
        val useCase = GetReportsUseCase(repository)

        val result = useCase(
            filter = ReportFilter(
                types = setOf(ProblemType.DUMP),
                statuses = setOf(ReportStatus.OPEN),
            ),
        ).first()

        assertEquals(listOf(openDump), result)
    }

    @Test
    fun getMyReportsUseCase_returnsOnlyCurrentUsersReports() = runBlocking {
        val myReport = testReport(id = "1", authorId = "user-1")
        val otherReport = testReport(id = "2", authorId = "user-2")
        val repository = FakeReportsRepository(observedReports = listOf(myReport, otherReport))
        val authRepository = FakeAuthRepository(session = session("user-1"))
        val useCase = GetMyReportsUseCase(repository, authRepository)

        val result = useCase().first()

        assertEquals(listOf(myReport), result)
    }

    @Test
    fun getMyReportsUseCase_returnsEmptyListForSignedOutUser() = runBlocking {
        val repository = FakeReportsRepository(observedReports = listOf(testReport(id = "1")))
        val authRepository = FakeAuthRepository(session = null)
        val useCase = GetMyReportsUseCase(repository, authRepository)

        val result = useCase().first()

        assertEquals(emptyList<Report>(), result)
    }

    private fun session(userId: String) = AuthSession(
        token = "token",
        userId = userId,
        email = "user@example.com",
        displayName = "User",
        role = "USER",
    )

    private fun testReport(
        id: String,
        type: ProblemType = ProblemType.DUMP,
        status: ReportStatus = ReportStatus.OPEN,
        authorId: String = "user",
    ): Report {
        return Report(
            id = id,
            title = "title-$id",
            description = "description-$id",
            type = type,
            status = status,
            latitude = 55.0,
            longitude = 37.0,
            authorId = authorId,
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            synced = false,
        )
    }
}

private class FakeReportsRepository(
    observedReports: List<Report> = emptyList(),
    private val reportById: Map<String, Report> = emptyMap(),
) : ReportsRepository {
    private val flow = flowOf(observedReports)

    var createdReport: Report? = null
        private set

    var deletedReportId: String? = null
        private set

    var statusChange: Pair<String, ReportStatus>? = null
        private set

    var syncPendingCalls: Int = 0
        private set

    override fun observeReports(): Flow<List<Report>> = flow

    override suspend fun getReportById(id: String): Report? = reportById[id]

    override suspend fun createReport(report: Report) {
        createdReport = report
    }

    override suspend fun updateStatus(id: String, status: ReportStatus) {
        statusChange = id to status
    }

    override suspend fun deleteReport(id: String) {
        deletedReportId = id
    }

    override suspend fun syncPending() {
        syncPendingCalls += 1
    }
}

private class FakeAuthRepository(private val session: AuthSession?) : AuthRepository {
    override suspend fun login(email: String, password: String): Result<AuthSession> =
        Result.failure(UnsupportedOperationException())

    override suspend fun register(
        name: String,
        email: String,
        password: String,
    ): Result<AuthSession> = Result.failure(UnsupportedOperationException())

    override fun currentSession(): AuthSession? = session

    override fun observeSession(): Flow<AuthSession?> = MutableStateFlow(session)

    override fun logout() = Unit
}
