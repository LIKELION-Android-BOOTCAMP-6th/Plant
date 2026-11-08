package com.a32b.plant.data.source.remote.auth

interface AuthRemoteDataSource {

    fun currentUid(): String?
    fun currentEmail(): String?
    fun hasSession(): Boolean
    fun getSignInProvider(): String?

    suspend fun signInWithGoogle(idToken: String): String
    suspend fun reauthenticateWithGoogle(idToken: String)
    /** ID 토큰을 서버에서 새로 받는다. 재인증 직후 새 auth_time을 토큰에 반영할 때 사용한다. */
    suspend fun refreshIdToken()
    /** 회원탈퇴 함수(deleteAccount)를 호출한다. 데이터·닉네임·Auth 계정 삭제는 서버가 처리한다. */
    suspend fun deleteAccount()
    fun signOut()
}
