package com.a32b.plant.domain.model

import com.a32b.plant.domain.session.SessionExpiredReason

data class SignInResult(
    val uid: String,
    val nickname: String,
    val isFirstLogin: Boolean
)

sealed class AutoLoginResult {
    data class LoggedIn(val uid: String, val user: User) : AutoLoginResult()
    object NotLoggedIn : AutoLoginResult()
    /** 앱 화면이 없는 동안 세션 만료가 감지되어 로그아웃한 경우. 로그인 화면에서 사유를 안내한다 (#509) */
    data class SessionExpired(val reason: SessionExpiredReason) : AutoLoginResult()
}
