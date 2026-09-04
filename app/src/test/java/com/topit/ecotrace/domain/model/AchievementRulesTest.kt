package com.topit.ecotrace.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant

class AchievementRulesTest {

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
    }

    @Test
    fun stats_areEmptyForUserWithoutReports() {
        val stats = AchievementRules.statsOf(emptyList())

        assertEquals(UserStats(), stats)
        assertEquals(1, stats.level)
        assertEquals(100, stats.pointsForNextLevel)
        assertEquals(0f, stats.levelProgress, 0.001f)
    }

    @Test
    fun stats_reportLevelAndProgressToNextLevel() {
        val stats = UserStats(reportsSubmitted = 7, problemsSolved = 2, ecoPoints = 245)

        assertEquals(3, stats.level)
        assertEquals(300, stats.pointsForNextLevel)
        assertEquals(0.45f, stats.levelProgress, 0.001f)
    }

    @Test
    fun eligible_isEmptyWithoutReports() {
        assertEquals(emptySet<AchievementCode>(), AchievementRules.eligible(emptyList()))
    }

    @Test
    fun eligible_unlocksFirstReportOnSingleReport() {
        assertEquals(setOf(AchievementCode.FIRST_REPORT), AchievementRules.eligible(listOf(report())))
    }

    @Test
    fun eligible_unlocksReporterOnTenReports() {
        val reports = List(10) { report() }

        assertTrue(AchievementCode.REPORTER_10 in AchievementRules.eligible(reports))
        assertFalse(AchievementCode.REPORTER_10 in AchievementRules.eligible(reports.drop(1)))
    }

    @Test
    fun eligible_unlocksForestDefenderOnFiveFallenTrees() {
        val trees = List(5) { report(type = ProblemType.FALLEN_TREE) }

        assertTrue(AchievementCode.FOREST_DEFENDER in AchievementRules.eligible(trees))
        assertFalse(AchievementCode.FOREST_DEFENDER in AchievementRules.eligible(trees.drop(1)))
    }

    @Test
    fun eligible_ignoresFallenTreeReportsOfOtherTypes() {
        val mixed = List(5) { report(type = ProblemType.DUMP) }

        assertFalse(AchievementCode.FOREST_DEFENDER in AchievementRules.eligible(mixed))
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
        authorId = "user-1",
        createdAt = Instant.parse("2026-01-01T00:00:00Z"),
        synced = false,
    )
}
