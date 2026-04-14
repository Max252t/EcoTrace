package com.ecotrace.backend.domain.model

import kotlinx.serialization.Serializable
import java.time.Instant

data class Report(
    val id: String,
    val title: String,
    val description: String,
    val type: ProblemType,
    val status: ReportStatus,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val authorId: String,
    val createdAt: Instant,
    val updatedAt: Instant,
)

enum class ProblemType { DUMP, ROAD_PIT, PIPE_RUPTURE, FALLEN_TREE }
enum class ReportStatus { OPEN, IN_PROGRESS, RESOLVED }

// ── DTOs ──────────────────────────────────────────────────

@Serializable
data class CreateReportRequest(
    val title: String,
    val description: String,
    val type: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String? = null,
)

@Serializable
data class UpdateStatusRequest(
    val status: String,
)

@Serializable
data class ReportResponse(
    val id: String,
    val title: String,
    val description: String,
    val type: String,
    val status: String,
    val latitude: Double,
    val longitude: Double,
    val imageUrl: String?,
    val authorId: String,
    val createdAt: String,
    val updatedAt: String,
)

fun Report.toResponse() = ReportResponse(
    id = id,
    title = title,
    description = description,
    type = type.name,
    status = status.name,
    latitude = latitude,
    longitude = longitude,
    imageUrl = imageUrl,
    authorId = authorId,
    createdAt = createdAt.toString(),
    updatedAt = updatedAt.toString(),
)
