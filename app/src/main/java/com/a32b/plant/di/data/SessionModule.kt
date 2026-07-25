package com.a32b.plant.di.data

import com.a32b.plant.data.session.SessionExpiredEvent
import com.a32b.plant.domain.session.SessionExpiredNotifier
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SessionModule {

    @Binds
    @Singleton
    abstract fun bindSessionExpiredNotifier(impl: SessionExpiredEvent) : SessionExpiredNotifier
}
