package com.a32b.plant.di.domain

import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.usecase.auth.CheckAutoLoginUseCase
import com.a32b.plant.domain.usecase.auth.DeleteAccountUseCase
import com.a32b.plant.domain.usecase.auth.ResolveUserSessionUseCase
import com.a32b.plant.domain.usecase.auth.SetNicknameUseCase
import com.a32b.plant.domain.usecase.auth.SignInWithEmailUseCase
import com.a32b.plant.domain.usecase.auth.SignInWithGoogleUseCase
import com.a32b.plant.domain.usecase.auth.SignOutUseCase
import com.a32b.plant.domain.usecase.auth.SignUpWithEmailUseCase
import com.a32b.plant.domain.usecase.community.AddCommentUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.usecase.studying.ObserveStudyingUseCase
import com.google.firebase.auth.FirebaseAuth
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
    fun provideResolveUserSessionUseCase(user: UserRepository) =
        ResolveUserSessionUseCase(user)

    @Provides
    @Singleton
    fun provideSignInWithEmailUseCase(auth: AuthRepository, resolveUserSession: ResolveUserSessionUseCase) =
        SignInWithEmailUseCase(auth, resolveUserSession)

    @Provides
    @Singleton
    fun provideSignInWithGoogleUseCase(auth: AuthRepository, resolveUserSession: ResolveUserSessionUseCase) =
        SignInWithGoogleUseCase(auth, resolveUserSession)

    @Provides
    @Singleton
    fun provideSignUpWithEmailUseCase(auth: AuthRepository) =
        SignUpWithEmailUseCase(auth)

    @Provides
    @Singleton
    fun provideSetNicknameUseCase(user: UserRepository) =
        SetNicknameUseCase(user)

    @Provides
    @Singleton
    fun provideCheckAutoLoginUseCase(auth: AuthRepository, user: UserRepository) =
        CheckAutoLoginUseCase(auth, user)

    @Provides
    @Singleton
    fun provideSignOutUseCase(auth: AuthRepository, user: UserRepository) =
        SignOutUseCase(auth, user)

    @Provides
    @Singleton
    fun provideDeleteAccountUseCase(auth: AuthRepository, user: UserRepository) =
        DeleteAccountUseCase(auth, user)

    @Provides
    @Singleton
    fun provideObserveStudyingUseCase(auth: AuthRepository, study: StudyingRepository) =
        ObserveStudyingUseCase(auth, study)

    @Provides
    @Singleton
    fun provideAddCommentUseCase(auth: AuthRepository, community : CommunityRepository, firebaseAuth: FirebaseAuth) =
        AddCommentUseCase(auth, community, firebaseAuth)

}
