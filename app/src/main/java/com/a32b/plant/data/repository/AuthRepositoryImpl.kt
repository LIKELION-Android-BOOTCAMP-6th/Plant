package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.error.toFunctionsAppError
import com.a32b.plant.data.source.remote.auth.AuthRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuthException
import com.google.firebase.auth.FirebaseAuthInvalidUserException
import javax.inject.Inject
import javax.inject.Singleton

private const val DELETE_ACCOUNT_RETRY_MESSAGE = "탈퇴 처리 중 문제가 발생했습니다. 다시 시도해주세요."

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource
) : AuthRepository {

    override fun currentUid(): String? = authRemoteDataSource.currentUid()

    override fun currentEmail(): String? = authRemoteDataSource.currentEmail()

    override fun hasSession(): Boolean = authRemoteDataSource.hasSession()

    override fun getSignInProvider(): String? = authRemoteDataSource.getSignInProvider()

    override suspend fun signInWithGoogle(idToken: String): Result<String> = safeRunCatching {
        authRemoteDataSource.signInWithGoogle(idToken)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleAuthError(e, "구글 로그인 실패")) }
    )

    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = safeRunCatching {
        authRemoteDataSource.reauthenticateWithGoogle(idToken)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Result.Failure(accountNotFoundOrNull(e) ?: handleAuthError(e, "구글 재인증 실패"))
        }
    )

    override suspend fun refreshIdToken(): Result<Unit> = safeRunCatching {
        authRemoteDataSource.refreshIdToken()
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            val error = accountNotFoundOrNull(e) ?: when (e) {
                // 방금 재인증한 뒤라 거의 없지만, 로그인 정보가 없으면 다시 로그인하도록 안내한다
                is IllegalStateException -> AppError.Auth()
                else -> handleAuthError(e, "토큰 갱신 실패")
            }
            Result.Failure(error)
        }
    )

    override suspend fun deleteAccount(): Result<Unit> = safeRunCatching {
        authRemoteDataSource.deleteAccount()
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Log.e("AuthRepository", "회원탈퇴 함수 호출 실패: ${e.message}", e)
            // 서버 내부 오류는 함수가 원인을 숨기고 보내므로, 다시 호출하면 이어서 처리된다는 안내로 바꾼다.
            // 네트워크·재인증 필요(failed-precondition 문구) 등은 그대로 전달한다.
            val error = when (val mapped = e.toFunctionsAppError()) {
                is AppError.Server, is AppError.Unknown -> AppError.Custom(DELETE_ACCOUNT_RETRY_MESSAGE)
                else -> mapped
            }
            Result.Failure(error)
        }
    )

    override fun signOut() {
        authRemoteDataSource.signOut()
    }

    /**
     * 서버에서 Auth 계정이 이미 지워진 경우(이전 탈퇴 요청은 성공했는데 응답만 받지 못함).
     * 재인증·토큰 갱신이 ERROR_USER_NOT_FOUND로 실패한다.
     */
    private fun accountNotFoundOrNull(e: Throwable): AppError? =
        if ((e as? FirebaseAuthInvalidUserException)?.errorCode == "ERROR_USER_NOT_FOUND") {
            Log.w("AuthRepository", "Auth 계정이 이미 삭제됨", e)
            AppError.AccountNotFound()
        } else {
            null
        }

    private fun handleAuthError(e: Throwable, logMessage: String): AppError {
        Log.e("AuthRepository", "$logMessage: ${e.message}", e)

        val firebaseEx = e as? FirebaseAuthException
        return when (firebaseEx?.errorCode) {
            "ERROR_USER_NOT_FOUND",
            "ERROR_WRONG_PASSWORD",
            "ERROR_INVALID_CREDENTIAL" -> AppError.Auth("계정 정보가 올바르지 않습니다.")
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
