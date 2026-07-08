package com.a32b.plant.di.domain

import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.usecase.auth.SignInWithEmailUseCase
import com.a32b.plant.domain.usecase.studying.ObserveStudyingUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class UseCaseModule {

    @Provides
    @Singleton
    fun provideSignInWithEmailUseCase(auth : AuthRepository, user : UserRepository) =
        SignInWithEmailUseCase(auth, user)

    @Provides
    @Singleton
    fun provideObserveStudyingUseCase(auth: AuthRepository, study: StudyingRepository) =
        ObserveStudyingUseCase(auth, study)
}