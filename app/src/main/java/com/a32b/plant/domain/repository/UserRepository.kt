package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {

    val currentUser : StateFlow<User?>
    fun setCurrentUser(user: User)
    fun clearCurrentUser()
    suspend fun updateLastSelectedPot(uid: String, potId: String)

    suspend fun getUser(uid: String): Result<User?>
    suspend fun createUser(uid: String): Result<User>
    suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit>
    suspend fun isNicknameTaken(nickname: String): Result<Boolean>
    suspend fun registerNickname(nickname: String): Result<Unit>
    suspend fun deleteNickname(nickname: String): Result<Unit>
    suspend fun deleteUserData(uid: String): Result<Unit>
}