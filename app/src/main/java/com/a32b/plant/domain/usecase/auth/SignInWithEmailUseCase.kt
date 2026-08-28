package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.SignInResult
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class SignInWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val resolveUserSession: ResolveUserSessionUseCase
) {
    suspend operator fun invoke(email: String, password: String): Result<SignInResult> {
        val signInResult = authRepository.signInWithEmail(email, password)
        if (signInResult is Result.Failure) return signInResult
        val uid = (signInResult as Result.Success).data

        if (!authRepository.isEmailVerified()) {
            authRepository.signOut()
            return Result.Failure(AppError.Auth("이메일을 인증해주세요."))
        }

        return resolveUserSession(uid)
    }
}