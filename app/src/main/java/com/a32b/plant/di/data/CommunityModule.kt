package com.a32b.plant.di.data

import com.a32b.plant.data.community.PostUpdateEvent
import com.a32b.plant.domain.community.PostUpdateNotifier
import com.a32b.plant.domain.community.PostUpdateObserver
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class CommunityModule {

    @Binds
    @Singleton
    abstract fun bindPostUpdateNotifier(impl: PostUpdateEvent): PostUpdateNotifier

    @Binds
    @Singleton
    abstract fun bindPostUpdateObserver(impl: PostUpdateEvent): PostUpdateObserver
}
