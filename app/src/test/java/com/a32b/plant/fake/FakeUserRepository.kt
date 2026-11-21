package com.a32b.plant.fake

import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredReason
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow

/**
 * 테스트 공용 [UserRepository]. 테스트에서 쓰지 않는 함수는 호출되면 실패한다.
 * 게이트(CompletableDeferred)를 넣으면 해당 요청은 게이트가 완료될 때까지 끝나지 않는다.
 * [callLog]를 넘기면 startUserSession·endUserSession 호출을 일어난 순서대로 기록한다
 * (다른 Fake와 같은 리스트를 넘기면 저장소 사이의 호출 순서를 확인할 수 있다).
 */
class FakeUserRepository(
    initialUser: User? = null,
    // null이면 전달받은 nickname 인자로 성공한 것처럼 돌려준다. 이미 완료된 상태(서버 닉네임 반환)는 Result.Success("다른 닉네임")으로 흉내 낸다
    var completeFirstLoginResult: Result<String>? = null,
    var completeFirstLoginGate: CompletableDeferred<Unit>? = null,
    var updateProfileResult: Result<Unit> = Result.Success(Unit),
    var updateProfileGate: CompletableDeferred<Unit>? = null,
    var updateSessionTokenGate: CompletableDeferred<Unit>? = null,
    var darkModeFlow: Flow<Boolean> = emptyFlow(),
    private val callLog: MutableList<String> = mutableListOf()
) : UserRepository {
    override val currentUser = MutableStateFlow(initialUser)

    val startedSessions = mutableListOf<User>()
    val darkModeUpdates = mutableListOf<Boolean>()

    /** completeFirstLogin 호출 기록 (uid to nickname) */
    val completedNicknames = mutableListOf<Pair<String, String>>()
    val updatedUsers = mutableListOf<User>()

    override fun observeDarkMode(): Flow<Boolean> = darkModeFlow

    override fun startUserSession(user: User) {
        callLog += "startUserSession"
        startedSessions += user
        currentUser.value = user
    }

    override fun endUserSession() {
        callLog += "endUserSession"
        currentUser.value = null
    }

    override fun takeExpiredSessionReason(): SessionExpiredReason? = null
    override suspend fun updateLastSelectedPot(uid: String, potId: String) = Unit

    override suspend fun completeFirstLogin(uid: String, nickname: String): Result<String> {
        completedNicknames += uid to nickname
        completeFirstLoginGate?.await()
        return completeFirstLoginResult ?: Result.Success(nickname)
    }

    override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> {
        darkModeUpdates += isDarkMode
        return Result.Success(Unit)
    }

    override suspend fun updateProfile(user: User): Result<Unit> {
        updatedUsers += user
        updateProfileGate?.await()
        return updateProfileResult
    }

    override suspend fun getUser(uid: String): Result<User?> = error("Unused")
    override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> = error("Unused")
    override suspend fun createUser(uid: String): Result<User> = error("Unused")
    override suspend fun checkAttendance(): Result<AttendanceDecision.Success> = error("Unused")

    /** updateSessionToken 호출 기록 (uid to token) */
    val sessionTokenUpdates = mutableListOf<Pair<String, String?>>()

    override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> {
        sessionTokenUpdates += uid to token
        updateSessionTokenGate?.await()
        return Result.Success(Unit)
    }
}
