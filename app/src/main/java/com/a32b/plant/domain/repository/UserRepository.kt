package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {

    val currentUser : StateFlow<User?>

    /** 로그인 세션을 시작한다. user를 즉시 반영하고, users/{uid} 문서를 실시간 구독해 계속 최신화한다. */
    fun startUserSession(user: User)

    /** 로그인 세션을 종료한다. 구독을 해제하고 currentUser를 비운다. */
    fun endUserSession()

    suspend fun updateLastSelectedPot(uid: String, potId: String)

    suspend fun getUser(uid: String): Result<User?>
    suspend fun createUser(uid: String): Result<User>
    suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit>
    suspend fun isNicknameTaken(nickname: String): Result<Boolean>
    suspend fun registerNickname(nickname: String): Result<Unit>
    suspend fun deleteNickname(nickname: String): Result<Unit>
    suspend fun deleteUserData(uid: String): Result<Unit>
}