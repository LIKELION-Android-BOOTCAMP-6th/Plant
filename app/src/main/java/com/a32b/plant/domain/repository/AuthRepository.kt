package com.a32b.plant.domain.repository

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.Result

interface AuthRepository {

    /** 현재 Firebase 세션의 uid. 세션 없으면 null. */
    fun currentUid(): String?
    /** 현재 Firebase 세션의 이메일. 세션 없으면 null. */
    fun currentEmail(): String?
    fun hasSession(): Boolean
    /** 로그인 제공자. "google.com" | null */
    fun getSignInProvider(): String?

    suspend fun signInWithGoogle(idToken: String): Result<String>
    /** 이미 Auth 계정이 삭제된 경우 [AppError.AccountNotFound]로 실패한다. */
    suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit>
    /**
     * ID 토큰을 서버에서 새로 받는다. 재인증 직후 호출해야 서버가 최근 재인증 여부(auth_time)를 확인할 수 있다.
     * 이미 Auth 계정이 삭제된 경우 [AppError.AccountNotFound]로 실패한다.
     */
    suspend fun refreshIdToken(): Result<Unit>
    /**
     * 회원탈퇴 함수를 호출한다. 서버가 데이터 → 닉네임 → users → Auth 계정 순으로 삭제하며,
     * 중간에 실패해도 다시 호출하면 남은 단계부터 이어서 처리된다.
     */
    suspend fun deleteAccount(): Result<Unit>

    /** 성공/실패와 무관하게 로그아웃되므로 Result를 사용하지 않는다. */
    fun signOut()
}
