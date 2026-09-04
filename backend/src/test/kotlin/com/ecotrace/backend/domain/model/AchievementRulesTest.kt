package com.ecotrace.backend.domain.model

import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AchievementRulesTest {

    private val now: Instant = Instant.parse("2026-05-01T12:00:00Z")

    @Test
    fun statsOf_countsReportsSolvedAndPoints() {
        val reports = listOf(
            report(status = ReportStatus.OPEN),
            report(status = ReportStatus.RESOLVED),
            report(status = ReportStatus.RESOLVED),
        )

        val stats = AchievementRules.statsOf(reports)

        assertEquals(3, stats.reportsSubmitted)
        assertEquals(2, stats.problemsSolved)
        assertEquals(3 * 20 + 2 * 30, stats.ecoPoints)
        assertEquals(2, stats.level)
    }

    @Test
    fun statsOf_returnsFirstLevelForEmptyHistory() {
        val stats = AchievementRules.statsOf(emptyList())

        assertEquals(0, stats.ecoPoints)
        assertEquals(1, stats.level)
    }

    @Test
    fun eligible_isEmptyWithoutReports() {
        assertEquals(emptySet<AchievementCode>(), AchievementRules.eligible(emptyList()))
    }

    @Test
    fun eligible_unlocksFirstReportOnSingleReport() {
        val eligible = AchievementRules.eligible(listOf(report()))

        assertEquals(setOf(AchievementCode.FIRST_REPORT), eligible)
    }

    @Test
    fun eligible_unlocksForestDefenderOnFiveFallenTrees() {
        val trees = List(5) { report(type = ProblemType.FALLEN_TREE) }

        val eligible = AchievementRules.eligible(trees)

        assertTrue(AchievementCode.FOREST_DEFENDER in eligible)
        assertFalse(AchievementCode.FOREST_DEFENDER in AchievementRules.eligible(trees.drop(1)))
    }

    @Test
    fun eligible_unlocksProblemSolverOnFiveResolvedReports() {
        val resolved = List(5) { report(status = ReportStatus.RESOLVED) }

        assertTrue(AchievementCode.PROBLEM_SOLVER in AchievementRules.eligible(resolved))
        assertFalse(AchievementCode.PROBLEM_SOLVER in AchievementRules.eligible(resolved.drop(1)))
    }

    @Test
    fun eligible_unlocksLevelFiveAtFourHundredPoints() {
        val resolved = List(8) { report(status = ReportStatus.RESOLVED) }

        assertEquals(400, AchievementRules.statsOf(resolved).ecoPoints)
        assertTrue(AchievementCode.LEVEL_5 in AchievementRules.eligible(resolved))
        assertFalse(AchievementCode.LEVEL_5 in AchievementRules.eligible(resolved.drop(1)))
    }

    @Test
    fun eligible_unlocksReporterOnTenReports() {
        val eligible = AchievementRules.eligible(List(10) { report() })

        assertTrue(AchievementCode.REPORTER_10 in eligible)
    }

    @Test
    fun merge_keepsOfflineUnlockTimeClaimedByClient() {
        val offlineUnlock = Instant.parse("2026-04-01T08:00:00Z")

        val merged = AchievementRules.merge(
            userId = USER,
            stored = emptyList(),
            claimed = listOf(Achievement(USER, AchievementCode.FIRST_REPORT, offlineUnlock)),
            eligible = setOf(AchievementCode.FIRST_REPORT),
            now = now,
        )

        assertEquals(listOf(offlineUnlock), merged.map { it.unlockedAt })
    }

    @Test
    fun merge_ignoresClaimThatServerDataDoesNotConfirm() {
        val merged = AchievementRules.merge(
            userId = USER,
            stored = emptyList(),
            claimed = listOf(Achievement(USER, AchievementCode.LEVEL_5, now)),
            eligible = emptySet(),
            now = now,
        )

        assertEquals(emptyList<Achievement>(), merged)
    }

    @Test
    fun merge_keepsEarliestUnlockTimeForStoredAchievement() {
        val stored = Achievement(USER, AchievementCode.FIRST_REPORT, Instant.parse("2026-04-10T00:00:00Z"))
        val earlier = Instant.parse("2026-04-02T00:00:00Z")
        val later = Instant.parse("2026-04-20T00:00:00Z")

        val withEarlier = AchievementRules.merge(
            userId = USER,
            stored = listOf(stored),
            claimed = listOf(Achievement(USER, AchievementCode.FIRST_REPORT, earlier)),
            eligible = setOf(AchievementCode.FIRST_REPORT),
            now = now,
        )
        val withLater = AchievementRules.merge(
            userId = USER,
            stored = listOf(stored),
            claimed = listOf(Achievement(USER, AchievementCode.FIRST_REPORT, later)),
            eligible = setOf(AchievementCode.FIRST_REPORT),
            now = now,
        )

        assertEquals(listOf(earlier), withEarlier.map { it.unlockedAt })
        assertEquals(listOf(stored.unlockedAt), withLater.map { it.unlockedAt })
    }

    @Test
    fun merge_clampsUnlockTimeFromTheFuture() {
        val merged = AchievementRules.merge(
            userId = USER,
            stored = emptyList(),
            claimed = listOf(Achievement(USER, AchievementCode.FIRST_REPORT, now.plusSeconds(86_400))),
            eligible = setOf(AchievementCode.FIRST_REPORT),
            now = now,
        )

        assertEquals(listOf(now), merged.map { it.unlockedAt })
    }

    @Test
    fun merge_unlocksEligibleAchievementsNotClaimedByClient() {
        val merged = AchievementRules.merge(
            userId = USER,
            stored = emptyList(),
            claimed = emptyList(),
            eligible = setOf(AchievementCode.FIRST_REPORT, AchievementCode.PROBLEM_SOLVER),
            now = now,
        )

        assertEquals(
            setOf(AchievementCode.FIRST_REPORT, AchievementCode.PROBLEM_SOLVER),
            merged.map { it.code }.toSet(),
        )
        assertTrue(merged.all { it.userId == USER && it.unlockedAt == now })
    }

    @Test
    fun merge_keepsStoredAchievementThatIsNoLongerEligible() {
        val stored = Achievement(USER, AchievementCode.PROBLEM_SOLVER, Instant.parse("2026-03-01T00:00:00Z"))

        val merged = AchievementRules.merge(
            userId = USER,
            stored = listOf(stored),
            claimed = emptyList(),
            eligible = emptySet(),
            now = now,
        )

        assertEquals(listOf(stored), merged)
    }

    private fun report(
        type: ProblemType = ProblemType.DUMP,
        status: ReportStatus = ReportStatus.OPEN,
    ) = Report(
        id = "report",
        title = "title",
        description = "description",
        type = type,
        status = status,
        latitude = 55.0,
        longitude = 37.0,
        imageUrl = null,
        authorId = USER,
        createdAt = now,
        updatedAt = now,
    )

    private companion object {
        const val USER = "user-1"
    }
}
