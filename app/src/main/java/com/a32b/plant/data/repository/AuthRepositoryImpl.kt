package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.source.remote.auth.AuthRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource
) : AuthRepository {

    override fun currentUid(): String? = authRemoteDataSource.currentUid()

    override fun currentEmail(): String? = authRemoteDataSource.currentEmail()

    override fun hasSession(): Boolean = authRemoteDataSource.hasSession()

    override fun isEmailVerified(): Boolean = authRemoteDataSource.isEmailVerified()

    override fun getSignInProvider(): String? = authRemoteDataSource.getSignInProvider()

    override suspend fun signInWithEmail(email: String, password: String): Result<String> = safeRunCatching {
        authRemoteDataSource.signInWithEmail(email, password)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "이메일 로그인 실패")) }
    )

    override suspend fun signInWithGoogle(idToken: String): Result<String> = safeRunCatching {
        authRemoteDataSource.signInWithGoogle(idToken)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "구글 로그인 실패")) }
    )

    override suspend fun signUpWithEmail(email: String, password: String): Result<String> = safeRunCatching {
        authRemoteDataSource.signUpWithEmail(email, password)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "회원가입 실패")) }
    )

    override suspend fun sendEmailVerification(): Result<Unit> = safeRunCatching {
        authRemoteDataSource.sendEmailVerification()
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "인증 메일 발송 실패")) }
    )

    override suspend fun sendPasswordResetEmail(email: String): Result<Unit> = safeRunCatching {
        authRemoteDataSource.sendPasswordResetEmail(email)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "비밀번호 재설정 메일 발송 실패")) }
    )

    override suspend fun reauthenticateWithEmail(email: String, password: String): Result<Unit> = safeRunCatching {
        authRemoteDataSource.reauthenticateWithEmail(email, password)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "이메일 재인증 실패")) }
    )

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = safeRunCatching {
        authRemoteDataSource.reauthenticateWithGoogle(idToken)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "구글 재인증 실패")) }
    )

    override suspend fun deleteAuthAccount(): Result<Unit> = safeRunCatching {
        authRemoteDataSource.deleteAuthAccount()
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            val error = if (e.message?.contains("RECENT_LOGIN_REQUIRED") == true) {
                Log.e("AuthRepository", "계정 삭제 실패 - 재인증 필요", e)
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
                // datasource에서 auth.currentUser가 null일 때 던지는 예외.
                // 의미적으로 "유저 없음"이므로 UnknownUser로 통일해 세션 만료 플로우를 탄다.
                is IllegalStateException -> AppError.UnknownUser()
                else -> AppError.Unknown()
            }
        }
    }
}
