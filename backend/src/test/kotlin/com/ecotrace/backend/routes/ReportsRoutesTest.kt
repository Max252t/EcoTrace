package com.ecotrace.backend.routes

import com.ecotrace.backend.data.storage.FileStorage
import com.ecotrace.backend.domain.model.ProblemType
import com.ecotrace.backend.domain.model.Report
import com.ecotrace.backend.domain.model.ReportStatus
import com.ecotrace.backend.plugins.configureSerialization
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.ktor.server.routing.routing
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import java.io.File
import java.nio.file.Files
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReportsRoutesTest {

    private val directory: File = Files.createTempDirectory("ecotrace-reports-route").toFile()
    private val reports = FakeReportsRepository(listOf(reportOf(AUTHOR)))
    private val uploads = RecordingUploadsRepository(mapOf(OWN_IMAGE to AUTHOR, FOREIGN_IMAGE to STRANGER))

    @AfterTest
    fun cleanUp() {
        directory.deleteRecursively()
    }

    @Test
    fun create_buildsImageUrlFromOwnUpload() = testApplication {
        installReportRoutes()

        val response = createReport(AUTHOR, imageUrl = "/api/files/$OWN_IMAGE")

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("/api/files/$OWN_IMAGE"))
    }

    @Test
    fun create_acceptsBareUploadName() = testApplication {
        installReportRoutes()

        val response = createReport(AUTHOR, imageUrl = OWN_IMAGE)

        assertEquals(HttpStatusCode.Created, response.status)
        assertTrue(response.bodyAsText().contains("/api/files/$OWN_IMAGE"))
    }

    @Test
    fun create_rejectsImageUploadedBySomeoneElse() = testApplication {
        installReportRoutes()

        val response = createReport(AUTHOR, imageUrl = "/api/files/$FOREIGN_IMAGE")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun create_rejectsExternalImageUrl() = testApplication {
        installReportRoutes()

        val response = createReport(AUTHOR, imageUrl = "https://tracker.example.com/pixel.png")

        assertEquals(HttpStatusCode.BadRequest, response.status)
    }

    @Test
    fun create_rejectsEmptyOrOverlongTitle() = testApplication {
        installReportRoutes()

        assertEquals(HttpStatusCode.BadRequest, createReport(AUTHOR, title = "  ").status)
        assertEquals(HttpStatusCode.BadRequest, createReport(AUTHOR, title = "t".repeat(256)).status)
    }

    @Test
    fun create_rejectsCoordinatesOutOfRange() = testApplication {
        installReportRoutes()

        assertEquals(HttpStatusCode.BadRequest, createReport(AUTHOR, latitude = 91.0).status)
        assertEquals(HttpStatusCode.BadRequest, createReport(AUTHOR, longitude = -181.0).status)
    }

    @Test
    fun patchStatus_allowsAuthorToTakeReportInProgress() = testApplication {
        installReportRoutes()

        val response = patchStatus(AUTHOR, ReportStatus.IN_PROGRESS)

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ReportStatus.IN_PROGRESS, reports.stored.getValue(REPORT_ID).status)
    }

    @Test
    fun patchStatus_forbidsAuthorFromResolvingOwnReport() = testApplication {
        installReportRoutes()

        val response = patchStatus(AUTHOR, ReportStatus.RESOLVED)

        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals(ReportStatus.OPEN, reports.stored.getValue(REPORT_ID).status)
    }

    @Test
    fun patchStatus_allowsAdminToResolveReport() = testApplication {
        installReportRoutes()

        val response = patchStatus(STRANGER, ReportStatus.RESOLVED, role = "ADMIN")

        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals(ReportStatus.RESOLVED, reports.stored.getValue(REPORT_ID).status)
    }

    @Test
    fun patchStatus_forbidsStrangerFromChangingStatus() = testApplication {
        installReportRoutes()

        assertEquals(HttpStatusCode.Forbidden, patchStatus(STRANGER, ReportStatus.IN_PROGRESS).status)
    }

    @Test
    fun delete_removesReportPhotoAndUploadRecord() = testApplication {
        val photo = File(directory, OWN_IMAGE).apply { writeBytes(ByteArray(4)) }
        reports.stored[REPORT_ID] = reportOf(AUTHOR).copy(imageUrl = "/api/files/$OWN_IMAGE")
        installReportRoutes()

        val response = client.delete("/api/reports/$REPORT_ID") {
            header(HttpHeaders.Authorization, "Bearer ${testToken(AUTHOR)}")
        }

        assertEquals(HttpStatusCode.NoContent, response.status)
        assertFalse(photo.exists())
        assertFalse(uploads.stored.containsKey(OWN_IMAGE))
    }

    @Test
    fun list_appliesLimitAndOffset() = testApplication {
        repeat(5) { index ->
            reports.stored["r-$index"] = reportOf(AUTHOR).copy(
                id = "r-$index",
                createdAt = Instant.parse("2026-01-0${index + 1}T00:00:00Z"),
            )
        }
        installReportRoutes()

        val body = client.get("/api/reports?limit=2").bodyAsText()

        assertEquals(2, Regex("\"id\"").findAll(body).count())
    }

    private fun ApplicationTestBuilder.installReportRoutes() {
        val storage = FileStorage(directory, maxFileSizeBytes = 1024)
        application {
            configureSerialization()
            installTestAuth()
            routing { reportsRoutes(reports, uploads, storage) }
        }
    }

    private suspend fun ApplicationTestBuilder.createReport(
        userId: String,
        title: String = "Dump",
        latitude: Double = 55.0,
        longitude: Double = 37.0,
        imageUrl: String? = null,
    ): HttpResponse = client.post("/api/reports") {
        header(HttpHeaders.Authorization, "Bearer ${testToken(userId)}")
        contentType(ContentType.Application.Json)
        val image = if (imageUrl == null) "null" else "\"$imageUrl\""
        setBody(
            """
            {"title":"$title","description":"d","type":"DUMP",
             "latitude":$latitude,"longitude":$longitude,"imageUrl":$image}
            """.trimIndent(),
        )
    }

    private suspend fun ApplicationTestBuilder.patchStatus(
        userId: String,
        status: ReportStatus,
        role: String = "USER",
    ): HttpResponse = client.patch("/api/reports/$REPORT_ID/status") {
        header(HttpHeaders.Authorization, "Bearer ${testToken(userId, role)}")
        contentType(ContentType.Application.Json)
        setBody("""{"status":"${status.name}"}""")
    }

    private companion object {
        const val AUTHOR = "user-1"
        const val STRANGER = "user-2"
        const val REPORT_ID = "report-1"
        const val OWN_IMAGE = "own-image.jpg"
        const val FOREIGN_IMAGE = "foreign-image.jpg"

        fun reportOf(authorId: String) = Report(
            id = REPORT_ID,
            title = "title",
            description = "description",
            type = ProblemType.DUMP,
            status = ReportStatus.OPEN,
            latitude = 55.0,
            longitude = 37.0,
            imageUrl = null,
            authorId = authorId,
            createdAt = Instant.parse("2026-01-01T00:00:00Z"),
            updatedAt = Instant.parse("2026-01-01T00:00:00Z"),
        )
    }
}
