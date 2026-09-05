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
    fun createDraftReport_usesCurrentUserAsAuthorAndReturnsTrue() = runTest {
        every { authRepository.currentSession() } returns SESSION
        val reportSlot = slot<Report>()
        coEvery { addReportUseCase(capture(reportSlot)) } just Runs
        val viewModel = AddReportViewModel(addReportUseCase, authRepository)

        val created = viewModel.createDraftReport(
            title = "Дамп",
            description = "Свалка мусора",
            type = ProblemType.DUMP,
            latitude = 55.0,
            longitude = 37.0,
            imageUri = "content://media/1",
        )

        assertTrue(created)
        val report = reportSlot.captured
        assertEquals("Дамп", report.title)
        assertEquals("Свалка мусора", report.description)
        assertEquals(ProblemType.DUMP, report.type)
        assertEquals(55.0, report.latitude, 0.0)
        assertEquals(37.0, report.longitude, 0.0)
        assertEquals("content://media/1", report.imageUri)
        assertEquals(SESSION.userId, report.authorId)
    }

    @Test
    fun createDraftReport_returnsFalseAndSkipsCreationWhenSignedOut() = runTest {
        every { authRepository.currentSession() } returns null
        val viewModel = AddReportViewModel(addReportUseCase, authRepository)

        val created = viewModel.createDraftReport(
            title = "title",
            description = "description",
            type = ProblemType.ROAD_PIT,
            latitude = 10.0,
            longitude = 20.0,
            imageUri = null,
        )

        assertFalse(created)
        coVerify(exactly = 0) { addReportUseCase(any()) }
    }

    private companion object {
        val SESSION = AuthSession(
            token = "token",
            userId = "user-1",
            email = "user@example.com",
            displayName = "User",
            role = "USER",
        )
    }
}
