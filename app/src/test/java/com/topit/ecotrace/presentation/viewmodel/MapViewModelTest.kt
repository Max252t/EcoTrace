package com.topit.ecotrace.presentation.viewmodel

import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportFilter
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.usecase.GetReportsUseCase
import com.topit.ecotrace.domain.usecase.SyncReportsUseCase
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
class MapViewModelTest {

    private val getReportsUseCase: GetReportsUseCase = mockk()
    private val syncReportsUseCase: SyncReportsUseCase = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { syncReportsUseCase() } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun init_syncsReportsOnce() = runTest {
        every { getReportsUseCase(any()) } returns flowOf(emptyList())

        MapViewModel(getReportsUseCase, syncReportsUseCase)

        coVerify(exactly = 1) { syncReportsUseCase() }
    }

    @Test
    fun reports_reflectsResultsForDefaultFilter() = runTest {
        val report = testReport()
        every { getReportsUseCase(ReportFilter()) } returns flowOf(listOf(report))
        val viewModel = MapViewModel(getReportsUseCase, syncReportsUseCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.reports.collect {}
        }

        assertEquals(listOf(report), viewModel.reports.value)
        assertEquals(ReportFilter(), viewModel.filter.value)
    }

    @Test
    fun updateFilter_reloadsReportsForNewFilter() = runTest {
        val dumpReport = testReport(type = ProblemType.DUMP)
        val treeReport = testReport(type = ProblemType.FALLEN_TREE)
        val newFilter = ReportFilter(types = setOf(ProblemType.FALLEN_TREE))
        every { getReportsUseCase(ReportFilter()) } returns flowOf(listOf(dumpReport, treeReport))
        every { getReportsUseCase(newFilter) } returns flowOf(listOf(treeReport))
        val viewModel = MapViewModel(getReportsUseCase, syncReportsUseCase)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.reports.collect {}
        }

        viewModel.updateFilter(newFilter)

        assertEquals(newFilter, viewModel.filter.value)
        assertEquals(listOf(treeReport), viewModel.reports.value)
    }

    private fun testReport(type: ProblemType = ProblemType.DUMP, status: ReportStatus = ReportStatus.OPEN) = Report(
        id = "report-1",
        title = "title",
        description = "description",
        type = type,
        status = status,
        latitude = 55.0,
        longitude = 37.0,
        authorId = "user-1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        synced = true,
    )
}
