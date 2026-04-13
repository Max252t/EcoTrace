package com.topit.ecotrace.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [ReportEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class EcoTraceDatabase : RoomDatabase() {
    abstract fun reportsDao(): ReportsDao
}
