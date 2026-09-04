package com.topit.ecotrace.presentation.viewmodel

import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.AuthSession
import com.topit.ecotrace.domain.usecase.AddReportUseCase
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddReportViewModelTest {

    private val addReportUseCase: AddReportUseCase = mockk()
    private val authRepository: AuthRepository = mockk()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        coEvery { addReportUseCase(any()) } just Runs
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun createDraftReport_storesReportWithIdOfSignedInUser() = runTest {
        every { authRepository.currentSession() } returns SESSION
        val report = slot<Report>()
        coEvery { addReportUseCase(capture(report)) } just Runs

        val created = viewModel().createDraftReport(
            title = "Dump",
            description = "Near the park",
            type = ProblemType.DUMP,
            latitude = 55.0,
            longitude = 37.0,
            imageUri = null,
        )

        assertTrue(created)
        assertEquals(SESSION.userId, report.captured.authorId)
        assertEquals("Dump", report.captured.title)
    }

    @Test
    fun createDraftReport_isRejectedWhenSessionIsGone() = runTest {
        every { authRepository.currentSession() } returns null

        val created = viewModel().createDraftReport(
            title = "Dump",
            description = "Near the park",
            type = ProblemType.DUMP,
            latitude = 55.0,
            longitude = 37.0,
            imageUri = null,
        )

        assertFalse(created)
        coVerify(exactly = 0) { addReportUseCase(any()) }
    }

    private fun viewModel() = AddReportViewModel(addReportUseCase, authRepository)

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
