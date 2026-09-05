package com.topit.ecotrace.data.mapper

import com.topit.ecotrace.data.remote.api.AuthResponseDto
import com.topit.ecotrace.data.remote.api.CreateReportRequestDto
import com.topit.ecotrace.data.remote.api.ReportResponseDto
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.repository.AuthSession
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class BackendMappersTest {

    @Test
    fun authResponseDtoToDomain_mapsAllFields() {
        val dto = AuthResponseDto(
            token = "token",
            userId = "user-1",
            email = "user@example.com",
            displayName = "User",
            role = "USER",
        )

        assertEquals(
            AuthSession(
                token = "token",
                userId = "user-1",
                email = "user@example.com",
                displayName = "User",
                role = "USER",
            ),
            dto.toDomain(),
        )
    }

    @Test
    fun reportResponseDtoToDomain_mapsKnownTypeStatusAndDate() {
        val dto = reportResponseDto(
            type = ProblemType.FALLEN_TREE.name,
            status = ReportStatus.IN_PROGRESS.name,
            createdAt = "2026-05-01T10:00:00Z",
        )

        val report = dto.toDomain()

        assertEquals(ProblemType.FALLEN_TREE, report.type)
        assertEquals(ReportStatus.IN_PROGRESS, report.status)
        assertEquals(Instant.parse("2026-05-01T10:00:00Z"), report.createdAt)
        assertEquals(true, report.synced)
        assertEquals("photo.jpg", report.imageUri)
    }

    @Test
    fun reportResponseDtoToDomain_fallsBackToDumpForUnknownType() {
        val dto = reportResponseDto(type = "SOMETHING_NEW")

        assertEquals(ProblemType.DUMP, dto.toDomain().type)
    }

    @Test
    fun reportResponseDtoToDomain_fallsBackToOpenForUnknownStatus() {
        val dto = reportResponseDto(status = "SOMETHING_NEW")

        assertEquals(ReportStatus.OPEN, dto.toDomain().status)
    }

    @Test
    fun reportResponseDtoToDomain_fallsBackToNowForUnparsableDate() {
        val before = Instant.now()

        val dto = reportResponseDto(createdAt = "not-a-date")
        val createdAt = dto.toDomain().createdAt

        assertEquals(true, !createdAt.isBefore(before))
    }

    @Test
    fun reportToCreateRequest_mapsFieldsAndSerializesType() {
        val report = Report(
            id = "report-1",
            title = "title",
            description = "description",
            type = ProblemType.ROAD_PIT,
            latitude = 55.0,
            longitude = 37.0,
            imageUri = "content://media/1",
            authorId = "user-1",
        )

        val request = report.toCreateRequest()

        assertEquals(
            CreateReportRequestDto(
                title = "title",
                description = "description",
                type = ProblemType.ROAD_PIT.name,
                latitude = 55.0,
                longitude = 37.0,
                imageUrl = "content://media/1",
            ),
            request,
        )
    }

    private fun reportResponseDto(
        type: String = ProblemType.DUMP.name,
        status: String = ReportStatus.OPEN.name,
        createdAt: String = "2026-05-01T10:00:00Z",
    ) = ReportResponseDto(
        id = "report-1",
        title = "title",
        description = "description",
        type = type,
        status = status,
        latitude = 55.0,
        longitude = 37.0,
        imageUrl = "photo.jpg",
        authorId = "user-1",
        createdAt = createdAt,
        updatedAt = "2026-05-01T10:00:00Z",
    )
}
