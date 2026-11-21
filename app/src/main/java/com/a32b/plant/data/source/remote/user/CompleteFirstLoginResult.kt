package com.a32b.plant.data.source.remote.user

/** 첫 로그인 완료 트랜잭션이 확인한 서버 상태 */
sealed class CompleteFirstLoginResult {
    /** 닉네임을 등록하고 첫 로그인을 완료했다 */
    object Completed : CompleteFirstLoginResult()

    /**
     * 서버 문서의 isFirstLogin이 이미 false였다 (이전 시도가 시간 초과 뒤 반영됨 등). 아무것도 쓰지 않았다.
     * [nickname]은 서버에 저장돼 있는 닉네임(없으면 빈 문자열)으로, 이번에 입력한 닉네임과 다를 수 있다 (#661)
     */
    data class AlreadyCompleted(val nickname: String) : CompleteFirstLoginResult()

    /** 다른 사용자가 이미 쓰고 있는 닉네임이다 */
    object NicknameTaken : CompleteFirstLoginResult()
}
