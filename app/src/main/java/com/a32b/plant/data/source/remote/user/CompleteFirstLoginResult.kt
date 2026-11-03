package com.a32b.plant.data.source.remote.user

/** 첫 로그인 완료 트랜잭션이 확인한 서버 상태 */
sealed class CompleteFirstLoginResult {
    /** 닉네임을 등록하고 첫 로그인을 완료했다 */
    object Completed : CompleteFirstLoginResult()

    /** 이전 시도가 서버에 이미 반영돼 있었다 (내 닉네임 + 첫 로그인 완료 상태) */
    object AlreadyCompleted : CompleteFirstLoginResult()

    /** 다른 사용자가 이미 쓰고 있는 닉네임이다 */
    object NicknameTaken : CompleteFirstLoginResult()
}
