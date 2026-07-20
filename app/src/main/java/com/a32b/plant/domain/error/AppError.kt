package com.a32b.plant.domain.error

sealed class AppError : Throwable() {
    data class Network(override val message: String = "네트워크 연결이 원활하지 않습니다.") : AppError()
    data class Auth(override val message: String = "인증에 실패했습니다. 다시 로그인해주세요.") : AppError()
    data class Email(override val message: String = "이메일 형식이 올바르지 않습니다.") : AppError()
    data class Password(override val message: String = "비밀번호 형식이 올바르지 않습니다.") : AppError()
    data class Server(override val message: String = "서버에 오류가 발생했습니다.") : AppError()
    data class Unknown(override val message: String = "알 수 없는 오류가 발생했습니다.") : AppError()
    data class Update(override val message: String = "업데이트 실패했습니다.") : AppError()
    data class Custom(override val message: String) : AppError()
}