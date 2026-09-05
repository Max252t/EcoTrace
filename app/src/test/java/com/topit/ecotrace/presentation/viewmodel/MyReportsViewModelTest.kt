package com.topit.ecotrace.presentation.viewmodel

import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.usecase.DeleteReportUseCase
import com.topit.ecotrace.domain.usecase.GetMyReportsUseCase
import com.topit.ecotrace.domain.usecase.SyncReportsUseCase
import com.topit.ecotrace.domain.usecase.UpdateReportStatusUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class MyReportsViewModelTest {

    private val getMyReportsUseCase: GetMyReportsUseCase = mockk()
    private val syncReportsUseCase: SyncReportsUseCase = mockk()
    private val updateReportStatusUseCase: UpdateReportStatusUseCase = mockk()
    private val deleteReportUseCase: DeleteReportUseCase = mockk()
    private val authRepository: AuthRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { syncReportsUseCase() } just Runs
        coEvery { updateReportStatusUseCase(any(), any()) } just Runs
        coEvery { deleteReportUseCase(any()) } just Runs
        every { getMyReportsUseCase() } returns flowOf(emptyList())
        every { authRepository.currentSession() } returns null
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_syncsReportsOnce() = runTest {
        viewModel()

        coVerify(exactly = 1) { syncReportsUseCase() }
    }

    @Test
    fun reports_reflectsUseCaseResult() = runTest {
        val report = testReport()
        every { getMyReportsUseCase() } returns flowOf(listOf(report))
        val vm = viewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.reports.collect {}
        }

        assertEquals(listOf(report), vm.reports.value)
    }

    @Test
    fun markResolved_delegatesToUseCaseWithId() = runTest {
        val vm = viewModel()

        vm.markResolved("report-1")

        coVerify(exactly = 1) { updateReportStatusUseCase("report-1", ReportStatus.RESOLVED) }
    }

    @Test
    fun deleteReport_delegatesToUseCaseWithId() = runTest {
        val vm = viewModel()

        vm.deleteReport("report-1")

        coVerify(exactly = 1) { deleteReportUseCase("report-1") }
    }

    private fun viewModel() = MyReportsViewModel(
        getMyReportsUseCase,
        authRepository,
        syncReportsUseCase,
        updateReportStatusUseCase,
        deleteReportUseCase,
    )

    private fun testReport() = Report(
        id = "report-1",
        title = "title",
        description = "description",
        type = ProblemType.DUMP,
        status = ReportStatus.OPEN,
        latitude = 55.0,
        longitude = 37.0,
        authorId = "user-1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        synced = true,
    )
}
