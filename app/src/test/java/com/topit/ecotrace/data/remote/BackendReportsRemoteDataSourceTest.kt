package com.topit.ecotrace.data.remote

import com.topit.ecotrace.data.local.SessionStorage
import com.topit.ecotrace.data.remote.api.CreateReportRequestDto
import com.topit.ecotrace.data.remote.api.ReportResponseDto
import com.topit.ecotrace.data.remote.api.ReportsApi
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class BackendReportsRemoteDataSourceTest {

    private val reportsApi: ReportsApi = mockk()
    private val sessionStorage: SessionStorage = mockk()
    private val imageUploader: ImageUploader = mockk()

    @Test
    fun createReport_uploadsLocalPhotoAndSendsItsServerUrl() = runBlocking {
        every { sessionStorage.token() } returns TOKEN
        every { imageUploader.isLocal(LOCAL_URI) } returns true
        coEvery { imageUploader.upload(LOCAL_URI) } returns ImageUploadResult.Success(REMOTE_URL)
        val request = slot<CreateReportRequestDto>()
        coEvery { reportsApi.createReport(capture(request)) } returns responseDto(REMOTE_URL)

        val created = dataSource().createReport(report(imageUri = LOCAL_URI))

        assertEquals(REMOTE_URL, request.captured.imageUrl)
        assertEquals(REMOTE_URL, created?.imageUri)
    }

    @Test
    fun createReport_keepsReportPendingWhenPhotoUploadFails() = runBlocking {
        every { sessionStorage.token() } returns TOKEN
        every { imageUploader.isLocal(LOCAL_URI) } returns true
        coEvery { imageUploader.upload(LOCAL_URI) } returns ImageUploadResult.Failed

        val created = dataSource().createReport(report(imageUri = LOCAL_URI))

        assertNull(created)
        coVerify(exactly = 0) { reportsApi.createReport(any()) }
    }

    @Test
    fun createReport_sendsReportWithoutPhotoWhenLocalFileIsGone() = runBlocking {
        every { sessionStorage.token() } returns TOKEN
        every { imageUploader.isLocal(LOCAL_URI) } returns true
        coEvery { imageUploader.upload(LOCAL_URI) } returns ImageUploadResult.Unavailable
        val request = slot<CreateReportRequestDto>()
        coEvery { reportsApi.createReport(capture(request)) } returns responseDto(null)

        val created = dataSource().createReport(report(imageUri = LOCAL_URI))

        assertNull(request.captured.imageUrl)
        assertNull(created?.imageUri)
    }

    @Test
    fun createReport_doesNotReuploadPhotoThatIsAlreadyOnTheServer() = runBlocking {
        every { sessionStorage.token() } returns TOKEN
        every { imageUploader.isLocal(REMOTE_URL) } returns false
        coEvery { reportsApi.createReport(any()) } returns responseDto(REMOTE_URL)

        dataSource().createReport(report(imageUri = REMOTE_URL))

        coVerify(exactly = 0) { imageUploader.upload(any()) }
    }

    @Test
    fun createReport_sendsReportWithoutPhoto() = runBlocking {
        every { sessionStorage.token() } returns TOKEN
        val request = slot<CreateReportRequestDto>()
        coEvery { reportsApi.createReport(capture(request)) } returns responseDto(null)

        dataSource().createReport(report(imageUri = null))

        assertNull(request.captured.imageUrl)
        coVerify(exactly = 0) { imageUploader.upload(any()) }
    }

    @Test
    fun createReport_isSkippedForSignedOutUser() = runBlocking {
        every { sessionStorage.token() } returns null

        assertNull(dataSource().createReport(report(imageUri = LOCAL_URI)))
        coVerify(exactly = 0) { imageUploader.upload(any()) }
        coVerify(exactly = 0) { reportsApi.createReport(any()) }
    }

    private fun dataSource() =
        BackendReportsRemoteDataSource(reportsApi, sessionStorage, imageUploader)

    private fun report(imageUri: String?) = Report(
        id = "r-1",
        title = "title",
        description = "description",
        type = ProblemType.DUMP,
        status = ReportStatus.OPEN,
        latitude = 55.0,
        longitude = 37.0,
        imageUri = imageUri,
        authorId = "user-1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        synced = false,
    )

    private fun responseDto(imageUrl: String?) = ReportResponseDto(
        id = "r-1",
        title = "title",
        description = "description",
        type = ProblemType.DUMP.name,
        status = ReportStatus.OPEN.name,
        latitude = 55.0,
        longitude = 37.0,
        imageUrl = imageUrl,
        authorId = "user-1",
        createdAt = "2026-01-01T00:00:00Z",
        updatedAt = "2026-01-01T00:00:00Z",
    )

    private companion object {
        const val TOKEN = "token"
        const val LOCAL_URI = "content://media/external/images/1"
        const val REMOTE_URL = "/api/files/a.jpg"
    }
}
