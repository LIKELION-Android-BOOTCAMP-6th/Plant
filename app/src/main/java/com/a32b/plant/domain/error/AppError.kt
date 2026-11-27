package com.a32b.plant.domain.error

sealed class AppError : Throwable() {
    // Throwable.message는 String? 이므로, AppError 타입으로 받았을 때도
    // error.message를 그대로 쓸 수 있도록 non-null로 좁혀서 재정의한다.
    abstract override val message: String

    data class Network(override val message: String = "인터넷 연결이 원활하지 않아요. 네트워크를 확인해주세요.") : AppError()
    data class Auth(override val message: String = "인증에 실패했어요. 다시 로그인해주세요.") : AppError()
    data class UnknownUser(override val message: String = "사용자 정보를 가져오지 못했어요. 다시 로그인해 주세요.") : AppError()
    data class Server(override val message: String = "서버에 오류가 발생했어요.") : AppError()
    data class Unknown(override val message: String = "알 수 없는 오류가 발생했어요.") : AppError()
    data class Upload(override val message: String = "저장에 실패했어요.") : AppError()
    data class Local(override val message: String = "로컬 저장 실패") : AppError()
    data class Permission(override val message: String = "권한이 없어요.") : AppError()
    data class NicknameTaken(override val message: String = "사용 중인 닉네임이에요.") : AppError()
    data class NicknameInvalid(override val message: String) : AppError()
    // 첫 로그인 닉네임 설정에서 필수 항목(만 14세 이상·이용약관·개인정보처리방침)에 모두 동의하지 않음 (#661)
    data class RequiredConsentMissing(override val message: String = "필수 항목에 동의해 주세요.") : AppError()
    data class Custom(override val message: String) : AppError()
    data class Debug(override val message: String) : AppError() //외부로 노출하지 않고 조용히 처리하는 오류일 경우
    data class InsufficientItems(override val message: String = "아이템이 부족해요."): AppError()
    // 서버에서 Auth 계정이 이미 삭제됨 — 회원탈퇴 응답을 받지 못한 뒤 다시 시도한 경우 탈퇴 완료로 처리한다
    data class AccountNotFound(override val message: String = "이미 탈퇴 처리된 계정이에요.") : AppError()
}

// 수정하려던 댓글이 이미 삭제된 경우의 안내 문구 — Repository(에러)와 ViewModel(사전 확인)이 함께 사용
const val COMMENT_ALREADY_DELETED_MESSAGE = "이미 삭제된 댓글이에요."
