package com.a32b.plant.data.source.remote.auth

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth
) : AuthRemoteDataSource {

    override fun currentUid(): String? = auth.currentUser?.uid

    override fun hasSession(): Boolean = auth.currentUser != null

    override fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified == true

    override suspend fun signInWithEmail(email: String, password: String): String {
        val result = auth.signInWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw IllegalStateException("uid를 가져오지 못했습니다.")
    }

    override suspend fun signInWithGoogle(idToken: String): String {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        val result = auth.signInWithCredential(credential).await()
        return result.user?.uid ?: throw IllegalStateException("uid를 가져오지 못했습니다.")
    }

    override suspend fun signUpWithEmail(email: String, password: String): String {
        val result = auth.createUserWithEmailAndPassword(email, password).await()
        return result.user?.uid ?: throw IllegalStateException("uid를 가져오지 못했습니다.")
    }

    override suspend fun sendEmailVerification() {
        auth.currentUser?.sendEmailVerification()?.await()
            ?: throw IllegalStateException("현재 유저가 없습니다.")
    }

    override suspend fun sendPasswordResetEmail(email: String) {
        auth.sendPasswordResetEmail(email).await()
    }

    override suspend fun deleteAuthAccount() {
        auth.currentUser?.delete()?.await()
            ?: throw IllegalStateException("현재 유저가 없습니다.")
    }

    override fun signOut() {
        auth.signOut()
    }
}
