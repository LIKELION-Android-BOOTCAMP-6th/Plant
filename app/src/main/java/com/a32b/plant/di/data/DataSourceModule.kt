package com.a32b.plant.di.data

import com.a32b.plant.data.datasource.StudyLogRemoteDataSource
import com.a32b.plant.data.datasource.StudyLogRemoteDataSourceImpl
import com.a32b.plant.data.source.remote.auth.AuthRemoteDataSource
import com.a32b.plant.data.source.remote.auth.AuthRemoteDataSourceImpl
import com.a32b.plant.data.source.remote.community.CommunityRemoteDataSource
import com.a32b.plant.data.source.remote.community.CommunityRemoteDataSourceImpl
import com.a32b.plant.data.source.remote.pot.PotRemoteDataSource
import com.a32b.plant.data.source.remote.pot.PotRemoteDataSourceImpl
import com.a32b.plant.data.source.remote.studying.StudyingRemoteDataSource
import com.a32b.plant.data.source.remote.studying.StudyingRemoteDataSourceImpl
import com.a32b.plant.data.source.remote.user.UserRemoteDataSource
import com.a32b.plant.data.source.remote.user.UserRemoteDataSourceImpl
import com.a32b.plant.data.source.local.StudyingLocalDataSource
import com.a32b.plant.data.source.local.StudyingLocalDataSourceImpl
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

    @Binds
    @Singleton
    abstract fun bindStudyLogRemoteDataSource(
        impl: StudyLogRemoteDataSourceImpl
    ): StudyLogRemoteDataSource

}
