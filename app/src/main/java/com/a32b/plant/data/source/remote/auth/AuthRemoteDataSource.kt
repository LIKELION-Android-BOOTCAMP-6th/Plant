package com.a32b.plant.data.source.remote.auth

interface AuthRemoteDataSource {

    fun currentUid(): String?
    fun currentEmail(): String?
    fun hasSession(): Boolean
    fun getSignInProvider(): String?

    suspend fun signInWithGoogle(idToken: String): String
    suspend fun reauthenticateWithGoogle(idToken: String)
    suspend fun deleteAuthAccount()
    fun signOut()
}
