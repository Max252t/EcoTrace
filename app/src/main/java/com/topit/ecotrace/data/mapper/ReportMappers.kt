package com.topit.ecotrace.data.mapper

import com.topit.ecotrace.data.local.ReportEntity
import com.topit.ecotrace.domain.model.ProblemType
import com.topit.ecotrace.domain.model.Report
import com.topit.ecotrace.domain.model.ReportStatus
import java.time.Instant

fun ReportEntity.toDomain(): Report {
    return Report(
        id = id,
        title = title,
        description = description,
        type = ProblemType.valueOf(type),
        status = ReportStatus.valueOf(status),
        latitude = latitude,
        longitude = longitude,
        imageUri = imageUri,
        authorId = authorId,
        createdAt = Instant.ofEpochSecond(createdAtEpochSeconds),
    )
}

fun Report.toEntity(synced: Boolean): ReportEntity {
    return ReportEntity(
        id = id,
        title = title,
        description = description,
        type = type.name,
        status = status.name,
        latitude = latitude,
        longitude = longitude,
        imageUri = imageUri,
        authorId = authorId,
        createdAtEpochSeconds = createdAt.epochSecond,
        synced = synced,
    )
}
