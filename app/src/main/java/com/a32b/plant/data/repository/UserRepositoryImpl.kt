package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.data.source.remote.user.UserRemoteDataSource
import com.a32b.plant.di.qualifier.ApplicationScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val db: FirebaseFirestore,
    @param:ApplicationScope private val scope: CoroutineScope
) : UserRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    private var sessionJob: Job? = null

    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override fun startUserSession(user: User) {
        // 초기값을 즉시 세팅해 HomeViewModel 등이 init에서 currentUser.value를
        // 곧바로 읽어도 null로 오인해 로그인 에러 처리하지 않도록 한다.
        _currentUser.value = user

        sessionJob?.cancel()
        sessionJob = scope.launch {
            userRemoteDataSource.observeUser(user.uid)
                // 로그아웃/회원탈퇴 시 인증 토큰이 먼저 무효화되면 서버가 PERMISSION_DENIED를
                // 반환할 수 있다. 리스너 에러가 이 스코프 밖으로 전파되면 앱 전체가 죽으므로
                // 여기서 흡수한다. (endUserSession으로 정상 종료되는 경로와는 별개의 방어선)
                .catch { e -> Log.e("UserRepository", "currentUser 실시간 구독 실패: ${e.message}", e) }
                .collect { dto ->
                    dto?.let { _currentUser.value = it.toDomain() }
                }
        }
    }

    override fun endUserSession() {
        sessionJob?.cancel()
        sessionJob = null
        _currentUser.value = null
    }

    override suspend fun updateLastSelectedPot(uid: String, potId: String) {
        db.collection("users").document(uid)
            .update("lastSelectedPotId", potId)
            .await() // Firebase 확장 함수 사용
    }

    override suspend fun getUser(uid: String): Result<User?> = runCatching {
        userRemoteDataSource.getUser(uid)?.toDomain()
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

    override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> =
        runCatching {
            userRemoteDataSource.completeFirstLogin(uid, nickname)
        }.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e -> Result.Failure(handleError(e, "첫 로그인 완료 처리 실패")) }
        )

    override suspend fun updateDarkMode(user: User): Result<Unit> = safeRunCatching {
        userRemoteDataSource.updateDarkMode(
            uid = user.uid,
            isDarkMode = user.isDarkMode
        )
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Result.Failure(handleError(e, "다크모드 설정 변경 실패"))
        }
    )

    override suspend fun updateProfile(user: User): Result<Unit> = safeRunCatching {
        userRemoteDataSource.updateProfile(
            uid = user.uid,
            nickname = user.nickname,
            profileImg = user.profileImg
        )
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Result.Failure(
                handleError(e, "프로필 수정에 실패했습니다.")
            )
        }
    )

    override suspend fun isNicknameTaken(nickname: String): Result<Boolean> = safeRunCatching {
        userRemoteDataSource.isNicknameTaken(nickname)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleError(e, "닉네임 중복 검사 실패")) }
    )

    override suspend fun registerNickname(nickname: String): Result<Unit> = safeRunCatching {
        userRemoteDataSource.registerNickname(nickname)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleError(e, "닉네임 등록 실패")) }
    )

    override suspend fun deleteNickname(nickname: String): Result<Unit> = safeRunCatching {
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
