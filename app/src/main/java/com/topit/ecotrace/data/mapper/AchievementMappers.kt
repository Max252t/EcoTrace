package com.topit.ecotrace.data.mapper

import com.topit.ecotrace.data.local.AchievementEntity
import com.topit.ecotrace.data.remote.api.AchievementDto
import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.model.AchievementCode
import java.time.Instant

fun AchievementEntity.toDomain(): Achievement? {
    val parsed = runCatching { AchievementCode.valueOf(code) }.getOrNull() ?: return null
    return Achievement(
        code = parsed,
        unlockedAt = Instant.ofEpochSecond(unlockedAtEpochSeconds),
        synced = synced,
    )
}

fun Achievement.toEntity(userId: String, synced: Boolean): AchievementEntity = AchievementEntity(
    userId = userId,
    code = code.name,
    unlockedAtEpochSeconds = unlockedAt.epochSecond,
    synced = synced,
)

fun AchievementDto.toDomain(): Achievement? {
    val parsed = runCatching { AchievementCode.valueOf(code) }.getOrNull() ?: return null
    val instant = runCatching { Instant.parse(unlockedAt) }.getOrNull() ?: return null
    return Achievement(code = parsed, unlockedAt = instant, synced = true)
}

fun Achievement.toDto(): AchievementDto = AchievementDto(
    code = code.name,
    unlockedAt = unlockedAt.toString(),
)
