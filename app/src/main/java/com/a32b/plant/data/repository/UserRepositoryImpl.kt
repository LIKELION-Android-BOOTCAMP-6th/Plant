package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.data.source.local.SettingsLocalDataSource
import com.a32b.plant.data.source.remote.user.CompleteFirstLoginResult
import com.a32b.plant.data.source.remote.user.UserRemoteDataSource
import com.a32b.plant.di.qualifier.ApplicationScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

private const val COMPLETE_FIRST_LOGIN_TIMEOUT_MS = 10_000L
private const val GET_USER_SERVER_TIMEOUT_MS = 2_000L
private const val SESSION_TOKEN_WRITE_TIMEOUT_MS = 1_500L

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource,
    private val settingsLocalDataSource: SettingsLocalDataSource,
    private val sessionExpiredNotifier: SessionExpiredNotifier,
    private val db: FirebaseFirestore,
    @param:ApplicationScope private val scope: CoroutineScope
) : UserRepository {
    private val _currentUser = MutableStateFlow<User?>(null)
    private var sessionJob: Job? = null
    @Volatile
    private var localSessionToken: String? = null
    // 읽기 실패와 재구독 사이에도 마지막 정상 테마를 유지한다. 영구 저장은 DataStore가 담당한다.
    private val lastKnownDarkMode = MutableStateFlow(false)

    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override fun observeDarkMode(): Flow<Boolean> =
        settingsLocalDataSource.isDarkMode
            // 저장한 적이 없으면 라이트 모드로 본다.
            .map { isDarkMode -> isDarkMode ?: false }
            .distinctUntilChanged()
            .onEach { isDarkMode -> lastKnownDarkMode.value = isDarkMode }
            .catch { e ->
                Log.e("UserRepository", "다크모드 설정을 불러오지 못했습니다.", e)
                // 최초 읽기 실패에도 값을 전달해 화면이 로딩 상태에 머물지 않게 한다.
                emit(lastKnownDarkMode.value)
            }

    override fun startUserSession(user: User) {
        // 초기값을 즉시 세팅해 HomeViewModel 등이 init에서 currentUser.value를
        // 곧바로 읽어도 null로 오인해 로그인 에러 처리하지 않도록 한다.
        _currentUser.value = user
        localSessionToken = user.activeSessionToken

        sessionJob?.cancel()
        sessionJob = scope.launch {
            userRemoteDataSource.observeUser(user.uid)
                // 로그아웃/회원탈퇴 시 인증 토큰이 먼저 무효화되면 서버가 PERMISSION_DENIED를
                // 반환할 수 있다. 리스너 에러가 이 스코프 밖으로 전파되면 앱 전체가 죽으므로
                // 여기서 흡수한다. (endUserSession으로 정상 종료되는 경로와는 별개의 방어선)
                .catch { e -> Log.e("UserRepository", "currentUser 실시간 구독 실패: ${e.message}", e) }
                .collect { dto ->
                    if (dto == null) {
                        // Firestore 유저 문서가 삭제된 경우. 리스너를 먼저 종료해야
                        // 이후 스냅샷에서 이벤트가 반복 발생하지 않는다.
                        endUserSession()
                        sessionExpiredNotifier.notifySessionExpired(SessionExpiredReason.USER_NOT_FOUND)
                        return@collect
                    }
                    val remoteToken = dto.activeSessionToken
                    val currentToken = localSessionToken
                    if (currentToken != null && remoteToken != null && currentToken != remoteToken) {
                        // 리스너를 먼저 종료해야 이후 스냅샷에서 이벤트가 반복 발생하지 않는다.
                        // MainActivity도 signOutUseCase()를 통해 endUserSession()을 재호출하나
                        // 멱등적이므로 문제없다. authRepository.signOut()은 그쪽에서 처리된다.
                        endUserSession()
                        sessionExpiredNotifier.notifySessionExpired(SessionExpiredReason.DUPLICATE_LOGIN)
                        return@collect
                    }
                    _currentUser.value = dto.toDomain()
                }
        }
    }

    override fun endUserSession() {
        sessionJob?.cancel()
        sessionJob = null
        _currentUser.value = null
        localSessionToken = null
    }

    override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> {
        // 일반 write의 await()는 서버 응답 전까지 끝나지 않으므로 호출부와 분리해 대기 시간만 제한한다.
        val write = scope.async {
            safeRunCatching {
                userRemoteDataSource.updateSessionToken(uid, token)
            }.onFailure {
                // 시간 초과 후 서버가 거부하면 이 토큰은 반영되지 않으므로 중복 로그인 비교에서 제외한다.
                if (token != null && localSessionToken == token) localSessionToken = null
            }
        }
        val result = withTimeoutOrNull(SESSION_TOKEN_WRITE_TIMEOUT_MS) { write.await() }
        if (result == null) {
            Log.w("UserRepository", "세션 토큰 저장 대기 시간 초과 (${SESSION_TOKEN_WRITE_TIMEOUT_MS}ms)")
            return Result.Success(Unit)
        }
        return result.fold(
            onSuccess = { Result.Success(Unit) },
            onFailure = { e -> Result.Failure(handleError(e, "세션 토큰 업데이트 실패")) }
        )
    }

    override suspend fun updateLastSelectedPot(uid: String, potId: String) {
        db.collection("users").document(uid)
            .update("lastSelectedPotId", potId)
            .await() // Firebase 확장 함수 사용
    }

    override suspend fun getUser(uid: String): Result<User?> = safeRunCatching {
        userRemoteDataSource.getUser(uid)?.toDomain()
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleError(e, "유저 정보 조회 실패")) }
    )

    override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> =
        safeRunCatching {
            // 문서가 없는 경우(user = null)와 구분되도록 시간 초과만 null로 받는다.
            withTimeoutOrNull(GET_USER_SERVER_TIMEOUT_MS) {
                UserSnapshot(userRemoteDataSource.getUserFromServer(uid)?.toDomain(), isFromServer = true)
            }
        }.fold(
            onSuccess = { snapshot ->
                if (snapshot != null) Result.Success(snapshot) else getUserFromCache(uid)
            },
            onFailure = { e ->
                if (isNetworkError(e)) getUserFromCache(uid)
                else Result.Failure(handleError(e, "유저 정보 조회 실패"))
            }
        )

    private suspend fun getUserFromCache(uid: String): Result<UserSnapshot> = safeRunCatching {
        UserSnapshot(userRemoteDataSource.getUserFromCache(uid)?.toDomain(), isFromServer = false)
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleError(e, "유저 정보 조회 실패")) }
    )

    private fun isNetworkError(e: Throwable): Boolean = when (e) {
        is FirebaseFirestoreException ->
            e.code == FirebaseFirestoreException.Code.UNAVAILABLE ||
                e.code == FirebaseFirestoreException.Code.DEADLINE_EXCEEDED
        is FirebaseNetworkException -> true
        else -> false
    }

    override suspend fun createUser(uid: String): Result<User> = safeRunCatching {
        val newUser = User.create().copy(uid = uid)
        userRemoteDataSource.createUser(uid, newUser.toDto())
        newUser
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { e -> Result.Failure(handleError(e, "유저 생성 실패")) }
    )

    override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> =
        safeRunCatching {
            // 연결이 불안정하면 트랜잭션이 재시도하며 길어질 수 있어 상한을 둔다.
            // 시간 초과(null)는 네트워크 에러로 취급하며, 서버에 뒤늦게 반영돼도 재시도 시 AlreadyCompleted로 성공 처리된다.
            withTimeoutOrNull(COMPLETE_FIRST_LOGIN_TIMEOUT_MS) {
                userRemoteDataSource.completeFirstLogin(uid, nickname)
            }
        }.fold(
            onSuccess = { completion ->
                when (completion) {
                    CompleteFirstLoginResult.Completed,
                    CompleteFirstLoginResult.AlreadyCompleted -> Result.Success(Unit)

                    CompleteFirstLoginResult.NicknameTaken -> Result.Failure(AppError.NicknameTaken())

                    null -> {
                        Log.w("UserRepository", "첫 로그인 완료 처리 시간 초과 (${COMPLETE_FIRST_LOGIN_TIMEOUT_MS}ms)")
                        Result.Failure(AppError.Network())
                    }
                }
            },
            onFailure = { e -> Result.Failure(handleCompleteFirstLoginError(e)) }
        )

    override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> = safeRunCatching {
        settingsLocalDataSource.updateDarkMode(isDarkMode)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e ->
            Result.Failure(handleLocalError(e, "다크모드 설정을 저장하지 못했습니다."))
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
        onFailure = { e -> Result.Failure(handleNicknameCheckError(e)) }
    )

    override suspend fun registerNickname(uid: String, nickname: String): Result<Unit> = safeRunCatching {
        userRemoteDataSource.registerNickname(uid, nickname)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleRegisterNicknameError(e)) }
    )

    override suspend fun deleteNickname(nickname: String): Result<Unit> = safeRunCatching {
        userRemoteDataSource.deleteNickname(nickname)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleError(e, "닉네임 삭제 실패")) }
    )

    override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = safeRunCatching {
        userRemoteDataSource.checkAttendance(uid)
    }.fold(
        onSuccess = { decision ->
            when (decision) {
                is AttendanceDecision.Success -> Result.Success(decision)
                AttendanceDecision.AlreadyChecked ->
                    Result.Failure(AppError.Custom("이미 오늘 출석했습니다."))
                AttendanceDecision.MonthCompleted ->
                    Result.Failure(AppError.Custom("이번 달 출석을 모두 완료했습니다."))
            }
        },
        onFailure = { e -> Result.Failure(handleAttendanceError(e)) }
    )

    override suspend fun deleteUserData(uid: String): Result<Unit> = safeRunCatching {
        userRemoteDataSource.deleteUserData(uid)
    }.fold(
        onSuccess = { Result.Success(Unit) },
        onFailure = { e -> Result.Failure(handleError(e, "유저 데이터 삭제 실패")) }
    )


    private fun handleLocalError(e: Throwable, message: String): AppError {
        Log.e("UserRepository", "$message: ${e.message}", e)
        return AppError.Local(message)
    }

    private fun handleError(e: Throwable, logMessage: String): AppError {
        Log.e("UserRepository", "$logMessage: ${e.message}", e)

        return when (e) {
            is FirebaseNetworkException -> AppError.Network()
            else -> AppError.Custom(logMessage)
        }
    }

    private fun handleAttendanceError(e: Throwable): AppError {
        // safeRunCatching이 CancellationException을 이미 가로채 다시 던지므로,
        // 여기 e는 CancellationException일 수 없다 (fold()의 onFailure로 넘어오는 시점엔 항상 필터됨).
        Log.e("UserRepository", "출석 처리 실패: ${e.message}", e)

        return when (e) {
            is AppError -> e
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()
                FirebaseFirestoreException.Code.PERMISSION_DENIED ->
                    AppError.Permission("출석 처리에 실패했습니다.")
                else -> AppError.Custom("출석 처리 실패")
            }
            is FirebaseNetworkException -> AppError.Network()
            else -> AppError.Custom("출석 처리 실패")
        }
    }

    private fun handleCompleteFirstLoginError(e: Throwable): AppError {
        Log.e("UserRepository", "첫 로그인 완료 처리 실패: ${e.message}", e)

        return when (e) {
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()

                else -> AppError.Custom("닉네임 설정에 실패했습니다.")
            }

            is FirebaseNetworkException -> AppError.Network()
            else -> AppError.Custom("닉네임 설정에 실패했습니다.")
        }
    }

    private fun handleRegisterNicknameError(e: Throwable): AppError {
        Log.e("UserRepository", "닉네임 등록 실패: ${e.message}", e)

        return when (e) {
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()
                // 중복 검사 직후 다른 사용자가 먼저 등록하면, 이미 있는 문서 덮어쓰기라 보안 규칙이 거부한다
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.NicknameTaken()
                else -> AppError.Custom("닉네임 등록 실패")
            }
            is FirebaseNetworkException -> AppError.Network()
            else -> AppError.Custom("닉네임 등록 실패")
        }
    }

    private fun handleNicknameCheckError(e: Throwable): AppError {
        Log.e("UserRepository", "닉네임 중복 검사 실패: ${e.message}", e)

        return when (e) {
            is FirebaseFirestoreException -> when (e.code) {
                FirebaseFirestoreException.Code.UNAVAILABLE,
                FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()
                else -> AppError.Custom("닉네임 중복 검사 실패")
            }
            is FirebaseNetworkException -> AppError.Network()
            else -> AppError.Custom("닉네임 중복 검사 실패")
        }
    }
}
