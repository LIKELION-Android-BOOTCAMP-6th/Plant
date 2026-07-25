package com.a32b.plant.di.data

import com.a32b.plant.data.datasource.auth.AuthRemoteDataSource
import com.a32b.plant.data.datasource.auth.AuthRemoteDataSourceImpl
import com.a32b.plant.data.datasource.community.CommunityRemoteDataSource
import com.a32b.plant.data.datasource.community.CommunityRemoteDataSourceImpl
import com.a32b.plant.data.datasource.pot.PotRemoteDataSource
import com.a32b.plant.data.datasource.pot.PotRemoteDataSourceImpl
import com.a32b.plant.data.datasource.studying.StudyingRemoteDataSource
import com.a32b.plant.data.datasource.studying.StudyingRemoteDataSourceImpl
import com.a32b.plant.data.datasource.user.UserRemoteDataSource
import com.a32b.plant.data.datasource.user.UserRemoteDataSourceImpl
import com.a32b.plant.data.local.StudyingLocalDataSource
import com.a32b.plant.data.local.StudyingLocalDataSourceImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class DataSourceModule {
    @Binds
    @Singleton
    abstract fun bindAuthRemoteDataSource(impl: AuthRemoteDataSourceImpl) : AuthRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindCommunityRemoteDataSource(impl: CommunityRemoteDataSourceImpl) : CommunityRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindPotRemoteDataSource(impl: PotRemoteDataSourceImpl) : PotRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindStudyingRemoteDataSource(impl: StudyingRemoteDataSourceImpl) : StudyingRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindUserRemoteDataSource(impl: UserRemoteDataSourceImpl) : UserRemoteDataSource

    @Binds
    @Singleton
    abstract fun bindStudyingLocalDataSource(impl: StudyingLocalDataSourceImpl) : StudyingLocalDataSource
}