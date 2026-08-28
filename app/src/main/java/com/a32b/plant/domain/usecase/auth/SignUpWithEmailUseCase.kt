package com.a32b.plant.domain.usecase.auth

import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class SignUpWithEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): Result<Unit> {
        val signUpResult = authRepository.signUpWithEmail(email, password)
        if (signUpResult is Result.Failure) return signUpResult

        // 인증 메일 발송 성공/실패와 무관하게 인증 전까지 로그인은 차단한다.
        val verificationResult = authRepository.sendEmailVerification()
        authRepository.signOut()

        return verificationResult
    }
}
