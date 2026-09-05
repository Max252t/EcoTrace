package com.topit.ecotrace.presentation.viewmodel

import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.usecase.GetAuthorNameUseCase
import com.topit.ecotrace.domain.usecase.GetReportByIdUseCase
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import com.topit.ecotrace.domain.usecase.UpdateReportStatusUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ReportDetailsViewModelTest {

    private val getReportByIdUseCase: GetReportByIdUseCase = mockk()
    private val updateReportStatusUseCase: UpdateReportStatusUseCase = mockk()
    private val getAuthorNameUseCase: GetAuthorNameUseCase = mockk()

    private val authRepository: AuthRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        every { authRepository.currentSession() } returns SESSION
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun isLoading_isTrueBeforeAnyLoad() {
        assertTrue(viewModel().isLoading.value)
    }

    @Test
    fun load_populatesReportAndAuthorNameThenStopsLoading() = runTest {
        val report = testReport()
        coEvery { getReportByIdUseCase("report-1") } returns report
        coEvery { getAuthorNameUseCase(report.authorId) } returns "Мария"
        val vm = viewModel()

        vm.load("report-1")

        assertEquals(report, vm.report.value)
        assertEquals("Мария", vm.authorName.value)
        assertFalse(vm.isLoading.value)
    }

    @Test
    fun load_leavesAuthorNameNullWhenReportIsMissing() = runTest {
        coEvery { getReportByIdUseCase("missing") } returns null
        val vm = viewModel()

        vm.load("missing")

        assertNull(vm.report.value)
        assertNull(vm.authorName.value)
        assertFalse(vm.isLoading.value)
        coVerify(exactly = 0) { getAuthorNameUseCase(any()) }
    }

    @Test
    fun updateStatus_delegatesThenReloadsReport() = runTest {
        val inProgress = testReport(status = ReportStatus.IN_PROGRESS)
        coEvery { updateReportStatusUseCase("report-1", ReportStatus.IN_PROGRESS) } just Runs
        coEvery { getReportByIdUseCase("report-1") } returns inProgress
        val vm = viewModel()

        vm.updateStatus("report-1", ReportStatus.IN_PROGRESS)

        coVerify(exactly = 1) { updateReportStatusUseCase("report-1", ReportStatus.IN_PROGRESS) }
        assertEquals(inProgress, vm.report.value)
    }

    @Test
    fun load_offersOpenAndInProgressToTheAuthor() = runTest {
        val report = testReport()
        coEvery { getReportByIdUseCase("report-1") } returns report
        coEvery { getAuthorNameUseCase(report.authorId) } returns "Иван"
        val vm = viewModel()

        vm.load("report-1")

        assertEquals(
            listOf(ReportStatus.OPEN, ReportStatus.IN_PROGRESS),
            vm.availableStatuses.value,
        )
    }

    @Test
    fun load_forbidsStatusChangeForSomeoneElsesReport() = runTest {
        val report = testReport().copy(authorId = "someone-else")
        coEvery { getReportByIdUseCase("report-1") } returns report
        coEvery { getAuthorNameUseCase(report.authorId) } returns "Мария"
        val vm = viewModel()

        vm.load("report-1")

        assertTrue(vm.availableStatuses.value.isEmpty())
    }

    @Test
    fun load_offersEveryStatusToAdmin() = runTest {
        val report = testReport().copy(authorId = "someone-else")
        every { authRepository.currentSession() } returns SESSION.copy(role = "ADMIN")
        coEvery { getReportByIdUseCase("report-1") } returns report
        coEvery { getAuthorNameUseCase(report.authorId) } returns "Мария"
        val vm = viewModel()

        vm.load("report-1")

        assertEquals(ReportStatus.entries, vm.availableStatuses.value)
    }

    @Test
    fun load_forbidsStatusChangeForSignedOutUser() = runTest {
        val report = testReport()
        every { authRepository.currentSession() } returns null
        coEvery { getReportByIdUseCase("report-1") } returns report
        coEvery { getAuthorNameUseCase(report.authorId) } returns null
        val vm = viewModel()

        vm.load("report-1")

        assertTrue(vm.availableStatuses.value.isEmpty())
    }

    private fun viewModel() = ReportDetailsViewModel(
        getReportByIdUseCase,
        updateReportStatusUseCase,
        getAuthorNameUseCase,
        authRepository,
    )

    private fun testReport(status: ReportStatus = ReportStatus.OPEN) = Report(
        id = "report-1",
        title = "title",
        description = "description",
        type = ProblemType.DUMP,
        status = status,
        latitude = 55.0,
        longitude = 37.0,
        authorId = "user-1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        synced = true,
    )

    private companion object {
        val SESSION = AuthSession(
            token = "token",
            userId = "user-1",
            email = "user@example.com",
            displayName = "Иван",
            role = "USER",
        )
    }
}
