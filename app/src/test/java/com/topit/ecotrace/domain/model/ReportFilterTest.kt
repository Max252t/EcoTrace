package com.topit.ecotrace.domain.model

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class ReportFilterTest {

    @Test
    fun matches_returnsTrueWhenFilterIsEmpty() {
        val filter = ReportFilter()

        assertTrue(filter.matches(testReport(type = ProblemType.DUMP, status = ReportStatus.OPEN)))
    }

    @Test
    fun matches_filtersByProblemType() {
        val filter = ReportFilter(types = setOf(ProblemType.ROAD_PIT))

        assertTrue(filter.matches(testReport(type = ProblemType.ROAD_PIT, status = ReportStatus.OPEN)))
        assertFalse(filter.matches(testReport(type = ProblemType.DUMP, status = ReportStatus.OPEN)))
    }

    @Test
    fun matches_filtersByStatus() {
        val filter = ReportFilter(statuses = setOf(ReportStatus.RESOLVED))

        assertTrue(filter.matches(testReport(type = ProblemType.DUMP, status = ReportStatus.RESOLVED)))
        assertFalse(filter.matches(testReport(type = ProblemType.DUMP, status = ReportStatus.OPEN)))
    }

    @Test
    fun matches_requiresBothTypeAndStatusWhenBothProvided() {
        val filter = ReportFilter(
            types = setOf(ProblemType.FALLEN_TREE),
            statuses = setOf(ReportStatus.IN_PROGRESS),
        )

        assertTrue(filter.matches(testReport(type = ProblemType.FALLEN_TREE, status = ReportStatus.IN_PROGRESS)))
        assertFalse(filter.matches(testReport(type = ProblemType.FALLEN_TREE, status = ReportStatus.OPEN)))
        assertFalse(filter.matches(testReport(type = ProblemType.DUMP, status = ReportStatus.IN_PROGRESS)))
    }

    private fun testReport(type: ProblemType, status: ReportStatus): Report {
        return Report(
            id = "id-$type-$status",
            title = "title",
            description = "description",
            type = type,
            status = status,
            latitude = 0.0,
            longitude = 0.0,
            authorId = "author",
            createdAt = Instant.parse("2024-01-01T00:00:00Z"),
            synced = false,
        )
    }
}
