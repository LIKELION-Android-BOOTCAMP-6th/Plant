package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {

    val currentUser: StateFlow<User?>

    /** 계정과 무관한 기기 설정의 다크모드 여부를 구독한다. */
    fun observeDarkMode(): Flow<Boolean>

    /** 로그인 세션을 시작한다. user를 즉시 반영하고, users/{uid} 문서를 실시간 구독해 계속 최신화한다. */
    fun startUserSession(user: User)

    /** 로그인 세션을 종료한다. 구독을 해제하고 currentUser를 비운다. */
    fun endUserSession()

    suspend fun updateLastSelectedPot(uid: String, potId: String)

    suspend fun getUser(uid: String): Result<User?>
    /** 서버 조회를 먼저 시도하고, 오프라인이거나 시간이 초과되면 로컬 캐시에서 읽는다. */
    suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot>
    suspend fun createUser(uid: String): Result<User>
    /**
     * 닉네임 중복 검사 + 닉네임 등록 + 첫 로그인 완료를 한 번에(원자적으로) 처리한다.
     * 이미 같은 설정이 완료된 상태에서 다시 호출하면 성공으로 처리한다.
     *
     * 실패 시 [AppError.NicknameTaken] (다른 사용자가 사용 중), [AppError.Network] (오프라인·시간 초과),
     * [AppError.Custom] (그 외 저장 실패)를 반환한다.
     */
    suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit>

    suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit>
    suspend fun updateProfile(user: User): Result<Unit>
    suspend fun isNicknameTaken(nickname: String): Result<Boolean>
    /** 닉네임 선점 문서에 소유자 uid를 함께 저장한다. 보안 규칙상 소유자만 삭제할 수 있다. */
    suspend fun registerNickname(uid: String, nickname: String): Result<Unit>
    suspend fun deleteNickname(nickname: String): Result<Unit>
    suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success>
    /**
     * 세션 토큰을 저장한다. 제한 시간 안에 서버 응답이 없어도 쓰기는 큐에 남아 나중에 반영되므로 성공으로 반환한다.
     * 뒤늦게 반영되면 다른 기기를 로그아웃시킬 수 있으므로, 사용자가 직접 로그인한 경우가 아닌 자동 진입에서는
     * 서버 응답을 확인한 뒤에 호출할 것.
     */
    suspend fun updateSessionToken(uid: String, token: String?): Result<Unit>
    suspend fun deleteUserData(uid: String): Result<Unit>

}
