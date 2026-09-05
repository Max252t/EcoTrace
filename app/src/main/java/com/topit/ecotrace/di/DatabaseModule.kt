package com.topit.ecotrace.di

import android.content.Context
import androidx.room.Room
import com.topit.ecotrace.data.local.AchievementsDao
import com.topit.ecotrace.data.local.EcoTraceDatabase
import com.topit.ecotrace.data.local.ReportsDao
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(context: Context): EcoTraceDatabase {
        return Room.databaseBuilder(
            context,
            EcoTraceDatabase::class.java,
            "ecotrace.db",
        )
            .addMigrations(
                EcoTraceDatabase.MIGRATION_1_2,
                EcoTraceDatabase.MIGRATION_2_3,
                EcoTraceDatabase.MIGRATION_3_4,
            )
            .build()
    }

    @Provides
    fun provideReportsDao(database: EcoTraceDatabase): ReportsDao {
        return database.reportsDao()
    }

    @Provides
    fun provideAchievementsDao(database: EcoTraceDatabase): AchievementsDao {
        return database.achievementsDao()
    }
}
