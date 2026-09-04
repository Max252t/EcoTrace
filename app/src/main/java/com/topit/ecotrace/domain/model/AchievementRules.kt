package com.topit.ecotrace.domain.model

object AchievementRules {
    const val POINTS_PER_REPORT = 20
    const val POINTS_PER_SOLVED_REPORT = 30
    const val POINTS_PER_LEVEL = 100

    const val REPORTER_REPORTS = 10
    const val FOREST_DEFENDER_TREES = 5
    const val PROBLEM_SOLVER_REPORTS = 5
    const val TARGET_LEVEL = 5

    fun statsOf(reports: List<Report>): UserStats {
        val solved = reports.count { it.status == ReportStatus.RESOLVED }
        return UserStats(
            reportsSubmitted = reports.size,
            problemsSolved = solved,
            ecoPoints = reports.size * POINTS_PER_REPORT + solved * POINTS_PER_SOLVED_REPORT,
        )
    }

    fun eligible(reports: List<Report>): Set<AchievementCode> {
        val stats = statsOf(reports)
        val fallenTrees = reports.count { it.type == ProblemType.FALLEN_TREE }
        return buildSet {
            if (stats.reportsSubmitted >= 1) add(AchievementCode.FIRST_REPORT)
            if (stats.reportsSubmitted >= REPORTER_REPORTS) add(AchievementCode.REPORTER_10)
            if (fallenTrees >= FOREST_DEFENDER_TREES) add(AchievementCode.FOREST_DEFENDER)
            if (stats.problemsSolved >= PROBLEM_SOLVER_REPORTS) add(AchievementCode.PROBLEM_SOLVER)
            if (stats.level >= TARGET_LEVEL) add(AchievementCode.LEVEL_5)
        }
    }
}
