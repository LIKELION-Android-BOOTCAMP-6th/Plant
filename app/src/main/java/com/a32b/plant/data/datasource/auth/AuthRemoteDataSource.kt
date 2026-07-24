package com.a32b.plant.data.datasource.auth

interface AuthRemoteDataSource {

    fun currentUid(): String?
    fun hasSession(): Boolean
    fun isEmailVerified(): Boolean

    suspend fun signInWithEmail(email: String, password: String): String
    suspend fun signInWithGoogle(idToken: String): String
    suspend fun signUpWithEmail(email: String, password: String): String
    suspend fun sendEmailVerification()
    suspend fun sendPasswordResetEmail(email: String)
    suspend fun deleteAuthAccount()
    fun signOut()
}
