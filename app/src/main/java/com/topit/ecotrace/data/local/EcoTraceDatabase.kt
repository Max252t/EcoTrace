package com.topit.ecotrace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [ReportEntity::class, AchievementEntity::class],
    version = 3,
    exportSchema = false,
)
abstract class EcoTraceDatabase : RoomDatabase() {
    abstract fun reportsDao(): ReportsDao
    abstract fun achievementsDao(): AchievementsDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "CREATE TABLE IF NOT EXISTS `achievements` (" +
                        "`userId` TEXT NOT NULL, " +
                        "`code` TEXT NOT NULL, " +
                        "`unlockedAtEpochSeconds` INTEGER NOT NULL, " +
                        "`synced` INTEGER NOT NULL, " +
                        "PRIMARY KEY(`userId`, `code`))",
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE `reports` ADD COLUMN `pendingDeletion` INTEGER NOT NULL DEFAULT 0",
                )
            }
        }
    }
}
