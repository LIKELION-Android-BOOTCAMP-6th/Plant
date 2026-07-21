package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.datasource.auth.AuthRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource
) : AuthRepository {

    override fun currentUid(): String? = authRemoteDataSource.currentUid()

    override fun hasSession(): Boolean = authRemoteDataSource.hasSession()

    override fun isEmailVerified(): Boolean = authRemoteDataSource.isEmailVerified()

    override suspend fun signInWithEmail(email: String, password: String): Result<String> = runCatching {
        authRemoteDataSource.signInWithEmail(email, password)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "이메일 로그인 실패")) }
    )

    override suspend fun signInWithGoogle(idToken: String): Result<String> = runCatching {
        authRemoteDataSource.signInWithGoogle(idToken)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "구글 로그인 실패")) }
    )

    override suspend fun signUpWithEmail(email: String, password: String): Result<String> = runCatching {
        authRemoteDataSource.signUpWithEmail(email, password)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "회원가입 실패")) }
    )

    override suspend fun sendEmailVerification(): Result<Unit> = runCatching {
        authRemoteDataSource.sendEmailVerification()
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "인증 메일 발송 실패")) }
    )

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = runCatching {
        authRemoteDataSource.sendPasswordResetEmail(email)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "비밀번호 재설정 메일 발송 실패")) }
    )

    override suspend fun deleteAuthAccount(): Result<Unit> = runCatching {
        authRemoteDataSource.deleteAuthAccount()
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            if (e is CancellationException) throw e

            Log.e("AuthRepository", "계정 삭제 실패", e)

            val error = if (e.message?.contains("RECENT_LOGIN_REQUIRED") == true) {
                AppError.Custom("보안을 위해 재로그인 후 다시 시도해주세요.")
            } else {
                handleAuthError(e, "계정 삭제 실패")
            }
            Result.Failure(error)
        }
    )

    override fun signOut() {
        authRemoteDataSource.signOut()
    }

    private fun handleAuthError(e: Throwable, logMessage: String): AppError {
        if (e is CancellationException) throw e

        Log.e("AuthRepository", "$logMessage: ${e.message}", e)

        val firebaseEx = e as? FirebaseAuthException
        return when (firebaseEx?.errorCode) {
            "ERROR_INVALID_EMAIL" -> AppError.Email()
            "ERROR_WEAK_PASSWORD" -> AppError.Password()
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL" -> AppError.Auth("계정 정보가 올바르지 않습니다.")
            "ERROR_EMAIL_ALREADY_IN_USE" -> AppError.Custom("이미 등록된 계정입니다.")
            else -> when (e) {
                is FirebaseNetworkException -> AppError.Network()
                else -> AppError.Unknown()
            }
        }
    }
}
