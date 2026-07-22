package com.a32b.plant.domain.repository

import com.a32b.plant.domain.result.Result

interface AuthRepository {

    /** 현재 Firebase 세션의 uid. 세션 없으면 null. */
    fun currentUid(): String?
    fun hasSession(): Boolean
    fun isEmailVerified(): Boolean

    suspend fun signInWithEmail(email: String, password: String): Result<String>
    suspend fun signInWithGoogle(idToken: String): Result<String>
    suspend fun signUpWithEmail(email: String, password: String): Result<String>
    suspend fun sendEmailVerification(): Result<Unit>
    suspend fun sendPasswordResetEmail(email: String): Result<Unit>
    suspend fun deleteAuthAccount(): Result<Unit>

    /** 성공/실패와 무관하게 로그아웃되므로 Result를 사용하지 않는다. */
    fun signOut()
}
