package com.a32b.plant.data.source.remote.user

import com.a32b.plant.data.model.UserDto
import com.a32b.plant.data.model.AttendanceResultDto
import kotlinx.coroutines.flow.Flow

interface UserRemoteDataSource {

    suspend fun getUser(uid: String): UserDto?
    suspend fun getUserFromServer(uid: String): UserDto?
    suspend fun getUserFromCache(uid: String): UserDto?
    fun observeUser(uid: String): Flow<UserDto?>
    suspend fun createUser(uid: String, user: UserDto)
    /** 닉네임 등록 + 첫 로그인 완료를 한 트랜잭션으로 처리하고, 확인한 서버 상태를 반환 */
    suspend fun completeFirstLogin(uid: String, nickname: String): CompleteFirstLoginResult
    /** 닉네임 중복 확인·새 닉네임 문서 생성·이전 닉네임 문서 삭제와 users 수정을 한 트랜잭션으로 처리하고, 확인한 서버 상태를 반환 */
    suspend fun updateProfile(uid: String, nickname: String, profileImg: String): UpdateProfileResult
    /** 출석 체크 함수 호출. 판정·저장·보상 지급은 서버가 서버 시각 기준으로 한다 (#531) */
    suspend fun checkAttendance(): AttendanceResultDto
    suspend fun updateSessionToken(uid: String, token: String?)
}
