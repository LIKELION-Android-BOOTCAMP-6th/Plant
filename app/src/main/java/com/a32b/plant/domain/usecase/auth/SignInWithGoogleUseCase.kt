package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.domain.model.SignInResult
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class SignInWithGoogleUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val resolveUserSession: ResolveUserSessionUseCase
) {
    suspend operator fun invoke(idToken: String): Result<SignInResult> {
        val signInResult = authRepository.signInWithGoogle(idToken)
        if (signInResult is Result.Failure) return signInResult
        val uid = (signInResult as Result.Success).data

        return resolveUserSession(uid)
    }
}
