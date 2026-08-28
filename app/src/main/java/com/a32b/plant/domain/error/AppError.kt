package com.a32b.plant.domain.error

sealed class AppError : Throwable() {
    // Throwable.message는 String? 이므로, AppError 타입으로 받았을 때도
    // error.message를 그대로 쓸 수 있도록 non-null로 좁혀서 재정의한다.
    abstract override val message: String

    data class Network(override val message: String = "인터넷 연결이 원활하지 않습니다. 네트워크를 확인해주세요.") : AppError()
    data class Auth(override val message: String = "인증에 실패했습니다. 다시 로그인해주세요.") : AppError()
    data class UnknownUser(override val message: String = "사용자 정보를 가져오지 못했습니다. 다시 로그인해 주세요.") : AppError()
    data class Email(override val message: String = "이메일 형식이 올바르지 않습니다.") : AppError()
    data class Password(override val message: String = "비밀번호 형식이 올바르지 않습니다.") : AppError()
    data class Server(override val message: String = "서버에 오류가 발생했습니다.") : AppError()
    data class Unknown(override val message: String = "알 수 없는 오류가 발생했습니다.") : AppError()
    data class Upload(override val message: String = "저장에 실패했습니다.") : AppError()
    data class Local(override val message: String = "로컬 저장 실패") : AppError()
    data class Permission(override val message: String = "권한이 없습니다.") : AppError()
    data class Custom(override val message: String) : AppError()
}
