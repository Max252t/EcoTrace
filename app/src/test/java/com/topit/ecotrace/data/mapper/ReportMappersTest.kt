package com.topit.ecotrace.data.mapper

import com.topit.ecotrace.data.local.ReportEntity
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant

class ReportMappersTest {

    @Test
    fun entityToDomain_mapsAllFieldsAndParsesEnumsAndInstant() {
        val entity = ReportEntity(
            id = "report-1",
            title = "title",
            description = "description",
            type = ProblemType.FALLEN_TREE.name,
            status = ReportStatus.IN_PROGRESS.name,
            latitude = 55.0,
            longitude = 37.0,
            imageUri = "content://media/1",
            authorId = "user-1",
            createdAtEpochSeconds = 1_700_000_000L,
            synced = true,
        )

        val domain = entity.toDomain()

        assertEquals(
            Report(
                id = "report-1",
                title = "title",
                description = "description",
                type = ProblemType.FALLEN_TREE,
                status = ReportStatus.IN_PROGRESS,
                latitude = 55.0,
                longitude = 37.0,
                imageUri = "content://media/1",
                authorId = "user-1",
                createdAt = Instant.ofEpochSecond(1_700_000_000L),
                synced = true,
            ),
            domain,
        )
    }

    @Test
    fun entityToDomain_mapsNullImageUri() {
        val entity = ReportEntity(
            id = "report-1",
            title = "title",
            description = "description",
            type = ProblemType.DUMP.name,
            status = ReportStatus.OPEN.name,
            latitude = 55.0,
            longitude = 37.0,
            imageUri = null,
            authorId = "user-1",
            createdAtEpochSeconds = 1_700_000_000L,
            synced = false,
        )

        assertEquals(null, entity.toDomain().imageUri)
    }

    @Test
    fun reportToEntity_mapsAllFieldsAndSerializesEnumsAndInstant() {
        val report = Report(
            id = "report-2",
            title = "title",
            description = "description",
            type = ProblemType.PIPE_RUPTURE,
            status = ReportStatus.RESOLVED,
            latitude = 41.0,
            longitude = 21.0,
            imageUri = "content://media/2",
            authorId = "user-2",
            createdAt = Instant.ofEpochSecond(1_700_000_500L),
            synced = false,
        )

        val entity = report.toEntity(synced = true)

        assertEquals(
            ReportEntity(
                id = "report-2",
                title = "title",
                description = "description",
                type = ProblemType.PIPE_RUPTURE.name,
                status = ReportStatus.RESOLVED.name,
                latitude = 41.0,
                longitude = 21.0,
                imageUri = "content://media/2",
                authorId = "user-2",
                createdAtEpochSeconds = 1_700_000_500L,
                synced = true,
            ),
            entity,
        )
    }
}
