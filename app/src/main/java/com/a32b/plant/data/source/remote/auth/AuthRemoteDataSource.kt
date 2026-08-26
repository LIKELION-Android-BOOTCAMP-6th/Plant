package com.a32b.plant.data.source.remote.auth

interface AuthRemoteDataSource {

    fun currentUid(): String?
    fun currentEmail(): String?
    fun hasSession(): Boolean
    fun isEmailVerified(): Boolean
    fun getSignInProvider(): String?

    suspend fun signInWithEmail(email: String, password: String): String
    suspend fun signInWithGoogle(idToken: String): String
    suspend fun signUpWithEmail(email: String, password: String): String
    suspend fun sendEmailVerification()
    suspend fun sendPasswordResetEmail(email: String)
    suspend fun reauthenticateWithEmail(email: String, password: String)
    suspend fun reauthenticateWithGoogle(idToken: String)
    suspend fun deleteAuthAccount()
    fun signOut()
}
