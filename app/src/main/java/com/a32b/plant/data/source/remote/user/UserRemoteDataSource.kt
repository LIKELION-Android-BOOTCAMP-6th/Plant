package com.a32b.plant.data.source.remote.user

import com.a32b.plant.data.model.UserDto
import com.a32b.plant.domain.model.AttendanceDecision
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.Flow

interface UserRemoteDataSource {

    suspend fun getUser(uid: String, source: Source = Source.DEFAULT): UserDto?
    fun observeUser(uid: String): Flow<UserDto?>
    suspend fun createUser(uid: String, user: UserDto)
    suspend fun completeFirstLogin(uid: String, nickname: String)
    suspend fun updateDarkMode(uid: String, isDarkMode: Boolean)
    suspend fun updateProfile(uid: String, nickname: String, profileImg: String)
    suspend fun isNicknameTaken(nickname: String): Boolean
    suspend fun registerNickname(nickname: String)
    suspend fun deleteNickname(nickname: String)
    suspend fun checkAttendance(uid: String): AttendanceDecision
    suspend fun deleteUserData(uid: String)
}
