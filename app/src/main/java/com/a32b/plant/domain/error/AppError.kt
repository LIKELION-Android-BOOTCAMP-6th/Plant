package com.a32b.plant.domain.error

sealed class AppError : Throwable() {
    // Throwable.message는 String? 이므로, AppError 타입으로 받았을 때도
    // error.message를 그대로 쓸 수 있도록 non-null로 좁혀서 재정의한다.
    abstract override val message: String

    data class Network(override val message: String = "인터넷 연결이 원활하지 않습니다. 네트워크를 확인해주세요.") : AppError()
    data class Auth(override val message: String = "인증에 실패했습니다. 다시 로그인해주세요.") : AppError()
    data class UnknownUser(override val message: String = "사용자 정보를 가져오지 못했습니다. 다시 로그인해 주세요.") : AppError()
    data class Server(override val message: String = "서버에 오류가 발생했습니다.") : AppError()
    data class Unknown(override val message: String = "알 수 없는 오류가 발생했습니다.") : AppError()
    data class Upload(override val message: String = "저장에 실패했습니다.") : AppError()
    data class Local(override val message: String = "로컬 저장 실패") : AppError()
    data class Permission(override val message: String = "권한이 없습니다.") : AppError()
    data class NicknameTaken(override val message: String = "사용 중인 닉네임입니다.") : AppError()
    data class NicknameInvalid(override val message: String) : AppError()
    data class Custom(override val message: String) : AppError()
    data class Debug(override val message: String) : AppError() //외부로 노출하지 않고 조용히 처리하는 오류일 경우
    data class InsufficientItems(override val message: String = "아이템이 부족합니다."): AppError()
    // 서버에서 Auth 계정이 이미 삭제됨 — 회원탈퇴 응답을 받지 못한 뒤 다시 시도한 경우 탈퇴 완료로 처리한다
    data class AccountNotFound(override val message: String = "이미 탈퇴 처리된 계정입니다.") : AppError()
}

// 수정하려던 댓글이 이미 삭제된 경우의 안내 문구 — Repository(에러)와 ViewModel(사전 확인)이 함께 사용
const val COMMENT_ALREADY_DELETED_MESSAGE = "이미 삭제된 댓글입니다."
