package com.a32b.plant.di.data

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
    abstract fun bindAuthRepository()

    @Binds
    @Singleton
    abstract fun bindCommunityRepository()

    @Binds
    @Singleton
    abstract fun bindPotRepository()

    @Binds
    @Singleton
    abstract fun bindStudyingRepository()

    @Binds
    @Singleton
    abstract fun bindUserRepository()
}