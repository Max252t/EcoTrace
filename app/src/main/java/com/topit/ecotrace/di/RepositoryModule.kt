package com.topit.ecotrace.di

import com.topit.ecotrace.data.remote.ReportsRemoteDataSource
import com.topit.ecotrace.data.remote.SupabaseReportsRemoteDataSource
import com.topit.ecotrace.data.repository.BackendAuthRepository
import com.topit.ecotrace.domain.repository.AuthRepository
import com.topit.ecotrace.data.repository.OfflineFirstReportsRepository
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
    fun bindRemoteDataSource(impl: SupabaseReportsRemoteDataSource): ReportsRemoteDataSource

    @Binds
    @Singleton
    fun bindAuthRepository(impl: BackendAuthRepository): AuthRepository
}
