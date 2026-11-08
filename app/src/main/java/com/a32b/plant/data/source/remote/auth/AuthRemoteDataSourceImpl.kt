package com.a32b.plant.data.source.remote.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.functions.FirebaseFunctions
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

// 함수의 timeoutSeconds(functions/index.js DELETE_ACCOUNT_OPTIONS)보다 조금 길게 기다린다.
// 기본값(70초)이면 데이터가 많은 사용자는 서버가 처리 중인데도 앱이 먼저 실패로 끝난다.
private const val DELETE_ACCOUNT_TIMEOUT_SECONDS = 310L

@Singleton
class AuthRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val functions: FirebaseFunctions
) : AuthRemoteDataSource {

    override fun currentUid(): String? = auth.currentUser?.uid

    override fun currentEmail(): String? = auth.currentUser?.email

    override fun hasSession(): Boolean = auth.currentUser != null

    override fun getSignInProvider(): String? =
        auth.currentUser?.providerData
            ?.firstOrNull { it.providerId != "firebase" }
            ?.providerId

    override suspend fun signInWithGoogle(idToken: String): String {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user?.uid ?: throw IllegalStateException("uid를 가져오지 못했습니다.")
    }

    override suspend fun reauthenticateWithGoogle(idToken: String) {
        val user = auth.currentUser ?: throw IllegalStateException("현재 유저가 없습니다.")
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        user.reauthenticate(credential).await()
    }

    override suspend fun refreshIdToken() {
        val user = auth.currentUser ?: throw IllegalStateException("현재 유저가 없습니다.")
        user.getIdToken(true).await()
    }

    override suspend fun deleteAccount() {
        functions.getHttpsCallable("deleteAccount")
            .withTimeout(DELETE_ACCOUNT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .call()
            .await()
    }

    override fun signOut() {
        auth.signOut()
    }
}
