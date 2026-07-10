package com.a32b.plant.di.data

import com.a32b.plant.data.datasource.pot.PotRemoteDataSource
import com.a32b.plant.data.datasource.pot.PotRemoteDataSourceImpl
import com.a32b.plant.data.repository.AuthRepositoryImpl
import com.a32b.plant.data.repository.CommunityRepositoryImpl
import com.a32b.plant.data.repository.PotRepositoryImpl
import com.a32b.plant.data.repository.StudyingRepositoryImpl
import com.a32b.plant.data.repository.UserRepositoryImpl
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(impl: AuthRepositoryImpl) : AuthRepository

    @Binds
    @Singleton
    abstract fun bindCommunityRepository(impl: CommunityRepositoryImpl) : CommunityRepository

    @Binds
    @Singleton
    abstract fun bindPotRepository(impl: PotRepositoryImpl) : PotRepository

    @Binds
    @Singleton
    abstract fun bindStudyingRepository(impl: StudyingRepositoryImpl): StudyingRepository

    @Binds
    @Singleton
    abstract fun bindUserRepository(impl: UserRepositoryImpl) : UserRepository
}