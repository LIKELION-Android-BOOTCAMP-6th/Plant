package com.a32b.plant.fake

import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.result.Result

/**
 * 테스트 공용 [AuthRepository]. uid-1로 Google 로그인된 세션을 흉내 낸다.
 * [callLog]를 넘기면 refreshIdToken·deleteAccount·signOut 호출을 일어난 순서대로 기록한다
 * (다른 Fake와 같은 리스트를 넘기면 저장소 사이의 호출 순서를 확인할 수 있다).
 */
class FakeAuthRepository(
    var reauthenticationResult: Result<Unit> = Result.Success(Unit),
    var refreshResult: Result<Unit> = Result.Success(Unit),
    var deleteAccountResult: Result<Unit> = Result.Success(Unit),
    /** deleteAccount 안에서 결과를 돌려주기 전에 실행된다 (취소 예외 던지기 등) */
    var onDeleteAccount: suspend () -> Unit = {},
    private val callLog: MutableList<String> = mutableListOf()
) : AuthRepository {
    var deleteAccountCalls = 0
    var signOutCalls = 0

    override fun currentUid(): String? = "uid-1"
    override fun currentEmail(): String? = null
    override fun hasSession(): Boolean = true
    override fun getSignInProvider(): String? = "google.com"
    override suspend fun signInWithGoogle(idToken: String): Result<String> = error("Unused")
    override suspend fun reauthenticateWithGoogle(idToken: String): Result<Unit> = reauthenticationResult

    override suspend fun refreshIdToken(): Result<Unit> {
        callLog += "refreshIdToken"
        return refreshResult
    }

    override suspend fun deleteAccount(): Result<Unit> {
        callLog += "deleteAccount"
        deleteAccountCalls++
        onDeleteAccount()
        return deleteAccountResult
    }

    override fun signOut() {
        callLog += "signOut"
        signOutCalls++
    }
}
