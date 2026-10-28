package com.a32b.plant.data.source.remote.user

import com.a32b.plant.data.model.UserDto
import com.a32b.plant.domain.model.AttendanceDecision
import kotlinx.coroutines.flow.Flow

interface UserRemoteDataSource {

    suspend fun getUser(uid: String): UserDto?
    fun observeUser(uid: String): Flow<UserDto?>
    suspend fun createUser(uid: String, user: UserDto)
    /** 닉네임 등록 + 첫 로그인 완료를 원자적으로 처리. 이미 사용 중인 닉네임이면 false */
    suspend fun completeFirstLogin(uid: String, nickname: String): Boolean
    suspend fun updateProfile(uid: String, nickname: String, profileImg: String)
    suspend fun isNicknameTaken(nickname: String): Boolean
    suspend fun registerNickname(nickname: String)
    suspend fun deleteNickname(nickname: String)
    suspend fun checkAttendance(uid: String): AttendanceDecision
    suspend fun updateSessionToken(uid: String, token: String?)
    suspend fun deleteUserData(uid: String)
}
