package com.a32b.plant.data.source.remote.user

import com.a32b.plant.data.model.UserDto
import com.a32b.plant.domain.model.AttendanceDecision
import kotlinx.coroutines.flow.Flow

interface UserRemoteDataSource {

    suspend fun getUser(uid: String): UserDto?
    suspend fun getUserFromServer(uid: String): UserDto?
    suspend fun getUserFromCache(uid: String): UserDto?
    fun observeUser(uid: String): Flow<UserDto?>
    suspend fun createUser(uid: String, user: UserDto)
    /** 닉네임 등록 + 첫 로그인 완료를 한 트랜잭션으로 처리하고, 확인한 서버 상태를 반환 */
    suspend fun completeFirstLogin(uid: String, nickname: String): CompleteFirstLoginResult
    suspend fun updateProfile(uid: String, nickname: String, profileImg: String)
    suspend fun isNicknameTaken(nickname: String): Boolean
    suspend fun registerNickname(uid: String, nickname: String)
    suspend fun deleteNickname(nickname: String)
    suspend fun checkAttendance(uid: String): AttendanceDecision
    suspend fun updateSessionToken(uid: String, token: String?)
    suspend fun deleteUserData(uid: String)
}
