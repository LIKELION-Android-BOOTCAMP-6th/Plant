package com.a32b.plant.data.datasource.user

import com.a32b.plant.data.model.UserDto

interface UserRemoteDataSource {

    suspend fun getUser(uid: String): UserDto?
    suspend fun createUser(uid: String, user: UserDto)
    suspend fun completeFirstLogin(uid: String, nickname: String)
    suspend fun isNicknameTaken(nickname: String): Boolean
    suspend fun registerNickname(nickname: String)
    suspend fun deleteNickname(nickname: String)
    suspend fun deleteUserData(uid: String)
}
