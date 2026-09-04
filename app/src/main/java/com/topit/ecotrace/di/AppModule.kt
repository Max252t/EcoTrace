package com.topit.ecotrace.di

import dagger.Module
import dagger.Provides
import java.time.Clock
import javax.inject.Singleton

@Module
object AppModule {
    @Provides
    @Singleton
    fun provideClock(): Clock = Clock.systemUTC()
}
