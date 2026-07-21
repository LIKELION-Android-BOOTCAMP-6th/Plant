package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.datasource.user.UserRemoteDataSource
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val db: FirebaseFirestore
): UserRepository {
    private val _currentUser = MutableStateFlow<User?>(null)

    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override fun setCurrentUser(user: User) {
        _currentUser.value = user
    }
    override fun clearCurrentUser() {
        _currentUser.value = null
    }

    override suspend fun updateLastSelectedPot(uid: String, potId: String) {
        db.collection("users").document(uid)
            .update("lastSelectedPotId", potId)
            .await() // Firebase 확장 함수 사용
    }

    override suspend fun getUser(uid: String): Result<User?> = runCatching {
        userRemoteDataSource.getUser(uid)?.toDomain(emptyList())
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleError(e, "유저 정보 조회 실패")) }
    )

    override suspend fun createUser(uid: String): Result<User> = runCatching {
        val newUser = User.create().copy(uid = uid)
        userRemoteDataSource.createUser(uid, newUser.toDto())
        newUser
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleError(e, "유저 생성 실패")) }
    )

    override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> = runCatching {
        userRemoteDataSource.completeFirstLogin(uid, nickname)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleError(e, "첫 로그인 완료 처리 실패")) }
    )

    override suspend fun isNicknameTaken(nickname: String): Result<Boolean> = runCatching {
        userRemoteDataSource.isNicknameTaken(nickname)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleError(e, "닉네임 중복 검사 실패")) }
    )

    override suspend fun registerNickname(nickname: String): Result<Unit> = runCatching {
        userRemoteDataSource.registerNickname(nickname)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleError(e, "닉네임 등록 실패")) }
    )

    override suspend fun deleteNickname(nickname: String): Result<Unit> = runCatching {
        userRemoteDataSource.deleteNickname(nickname)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleError(e, "닉네임 삭제 실패")) }
    )

    override suspend fun deleteUserData(uid: String): Result<Unit> = runCatching {
        userRemoteDataSource.deleteUserData(uid)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleError(e, "유저 데이터 삭제 실패")) }
    )

    private fun handleError(e: Throwable, logMessage: String): AppError {
        if (e is CancellationException) throw e

        Log.e("UserRepository", "$logMessage: ${e.message}", e)

        return when (e) {
            is FirebaseNetworkException -> AppError.Network()
            else -> AppError.Custom(logMessage)
        }
    }
}