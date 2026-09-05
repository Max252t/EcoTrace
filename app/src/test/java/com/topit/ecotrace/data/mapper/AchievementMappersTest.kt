package com.topit.ecotrace.data.mapper

import com.topit.ecotrace.data.local.AchievementEntity
import com.topit.ecotrace.data.remote.api.AchievementDto
import com.topit.ecotrace.domain.model.Achievement
import com.topit.ecotrace.domain.model.AchievementCode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.Instant

class AchievementMappersTest {

    @Test
    fun entityToDomain_mapsKnownCodeAndPreservesSyncedFlag() {
        val entity = AchievementEntity(
            userId = "user-1",
            code = AchievementCode.FIRST_REPORT.name,
            unlockedAtEpochSeconds = 1_700_000_000L,
            synced = true,
        )

        val domain = entity.toDomain()

        assertEquals(
            Achievement(
                code = AchievementCode.FIRST_REPORT,
                unlockedAt = Instant.ofEpochSecond(1_700_000_000L),
                synced = true,
            ),
            domain,
        )
    }

    @Test
    fun entityToDomain_returnsNullForUnknownCode() {
        val entity = AchievementEntity(
            userId = "user-1",
            code = "SOMETHING_NEW",
            unlockedAtEpochSeconds = 1_700_000_000L,
            synced = true,
        )

        assertNull(entity.toDomain())
    }

    @Test
    fun achievementToEntity_mapsUserIdCodeAndSyncedFlag() {
        val achievement = Achievement(
            code = AchievementCode.LEVEL_5,
            unlockedAt = Instant.ofEpochSecond(1_700_000_500L),
            synced = false,
        )

        val entity = achievement.toEntity(userId = "user-2", synced = true)

        assertEquals(
            AchievementEntity(
                userId = "user-2",
                code = AchievementCode.LEVEL_5.name,
                unlockedAtEpochSeconds = 1_700_000_500L,
                synced = true,
            ),
            entity,
        )
    }

    @Test
    fun dtoToDomain_mapsKnownCodeAndAlwaysMarksSynced() {
        val dto = AchievementDto(
            code = AchievementCode.PROBLEM_SOLVER.name,
            unlockedAt = "2026-05-01T10:00:00Z",
        )

        val domain = dto.toDomain()

        assertEquals(
            Achievement(
                code = AchievementCode.PROBLEM_SOLVER,
                unlockedAt = Instant.parse("2026-05-01T10:00:00Z"),
                synced = true,
            ),
            domain,
        )
    }

    @Test
    fun dtoToDomain_returnsNullForUnknownCode() {
        val dto = AchievementDto(code = "SOMETHING_NEW", unlockedAt = "2026-05-01T10:00:00Z")

        assertNull(dto.toDomain())
    }

    @Test
    fun dtoToDomain_returnsNullForUnparsableDate() {
        val dto = AchievementDto(code = AchievementCode.FIRST_REPORT.name, unlockedAt = "not-a-date")

        assertNull(dto.toDomain())
    }

    @Test
    fun achievementToDto_mapsCodeNameAndIsoInstant() {
        val achievement = Achievement(
            code = AchievementCode.FOREST_DEFENDER,
            unlockedAt = Instant.parse("2026-05-02T12:30:00Z"),
            synced = true,
        )

        val dto = achievement.toDto()

        assertEquals(AchievementCode.FOREST_DEFENDER.name, dto.code)
        assertEquals("2026-05-02T12:30:00Z", dto.unlockedAt)
    }
}
