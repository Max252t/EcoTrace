package com.topit.ecotrace.domain.model

import java.time.Instant
import java.util.UUID

data class Report(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val description: String,
    val type: ProblemType,
    val status: ReportStatus = ReportStatus.OPEN,
    val latitude: Double,
    val longitude: Double,
    val imageUri: String? = null,
    val authorId: String,
    val createdAt: Instant = Instant.now(),
)

enum class ProblemType {
    DUMP,
    ROAD_PIT,
    PIPE_RUPTURE,
    FALLEN_TREE,
}

enum class ReportStatus {
    OPEN,
    IN_PROGRESS,
    RESOLVED,
}

data class ReportFilter(
    val types: Set<ProblemType> = emptySet(),
    val statuses: Set<ReportStatus> = emptySet(),
) {
    fun matches(report: Report): Boolean {
        val typeMatches = types.isEmpty() || report.type in types
        val statusMatches = statuses.isEmpty() || report.status in statuses
        return typeMatches && statusMatches
    }
}
