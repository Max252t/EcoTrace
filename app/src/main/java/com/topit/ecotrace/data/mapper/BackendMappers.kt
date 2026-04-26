package com.topit.ecotrace.data.mapper

import com.topit.ecotrace.data.remote.api.AuthResponseDto
import com.topit.ecotrace.data.remote.api.CreateReportRequestDto
import com.topit.ecotrace.data.remote.api.ReportResponseDto
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import com.topit.ecotrace.domain.repository.AuthSession
import java.time.Instant

fun AuthResponseDto.toDomain(): AuthSession = AuthSession(
    token = token,
    userId = userId,
    email = email,
    displayName = displayName,
    role = role,
)

fun ReportResponseDto.toDomain(): Report = Report(
    id = id,
    title = title,
    description = description,
    type = runCatching { ProblemType.valueOf(type) }.getOrDefault(ProblemType.DUMP),
    status = runCatching { ReportStatus.valueOf(status) }.getOrDefault(ReportStatus.OPEN),
    latitude = latitude,
    longitude = longitude,
    imageUri = imageUrl,
    authorId = authorId,
    createdAt = runCatching { Instant.parse(createdAt) }.getOrDefault(Instant.now()),
    synced = true,
)

fun Report.toCreateRequest(): CreateReportRequestDto = CreateReportRequestDto(
    title = title,
    description = description,
    type = type.name,
    latitude = latitude,
    longitude = longitude,
    imageUrl = imageUri,
)
