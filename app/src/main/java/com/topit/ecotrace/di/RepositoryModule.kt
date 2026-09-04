package com.topit.ecotrace.di

import com.topit.ecotrace.data.remote.AchievementsRemoteDataSource
import com.topit.ecotrace.data.remote.BackendAchievementsRemoteDataSource
import com.topit.ecotrace.data.remote.ContentResolverImageSource
import com.topit.ecotrace.data.remote.LocalImageSource
import com.topit.ecotrace.data.remote.ReportsRemoteDataSource
import com.topit.ecotrace.data.remote.BackendReportsRemoteDataSource
import com.topit.ecotrace.data.repository.BackendAuthRepository
import com.topit.ecotrace.data.repository.BackendUsersRepository
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.domain.repository.UsersRepository
import com.topit.ecotrace.data.repository.OfflineFirstAchievementsRepository
import com.topit.ecotrace.data.repository.OfflineFirstReportsRepository
import com.topit.ecotrace.domain.repository.AchievementsRepository
import com.topit.ecotrace.domain.repository.ReportsRepository
import dagger.Binds
import dagger.Module
import javax.inject.Singleton

@Module
interface RepositoryModule {
    @Binds
    @Singleton
    fun bindReportsRepository(impl: OfflineFirstReportsRepository): ReportsRepository

    @Binds
    @Singleton
    fun bindRemoteDataSource(impl: BackendReportsRemoteDataSource): ReportsRemoteDataSource

    @Binds
    @Singleton
    fun bindAuthRepository(impl: BackendAuthRepository): AuthRepository

    @Binds
    @Singleton
    fun bindUsersRepository(impl: BackendUsersRepository): UsersRepository

    @Binds
    @Singleton
    fun bindLocalImageSource(impl: ContentResolverImageSource): LocalImageSource

    @Binds
    @Singleton
    fun bindAchievementsRepository(impl: OfflineFirstAchievementsRepository): AchievementsRepository

    @Binds
    @Singleton
    fun bindAchievementsRemoteDataSource(
        impl: BackendAchievementsRemoteDataSource,
    ): AchievementsRemoteDataSource
}
