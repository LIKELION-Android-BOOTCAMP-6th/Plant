package com.a32b.plant.domain.model

data class SignInResult(
    val uid: String,
    val nickname: String,
    val isFirstLogin: Boolean
)

sealed class AutoLoginResult {
    data class LoggedIn(val uid: String, val user: User) : AutoLoginResult()
    object NotLoggedIn : AutoLoginResult()
}
