package com.a32b.plant.data.source.remote.user

/** 프로필 변경 트랜잭션이 확인한 서버 상태 */
sealed class UpdateProfileResult {
    /** 닉네임 또는 프로필 이미지를 변경했다 */
    object Updated : UpdateProfileResult()

    /** 서버에 이미 같은 닉네임·프로필 이미지로 반영돼 있었다 (SDK 재시도 때 앞의 시도가 이미 반영된 경우 등) */
    object NoChange : UpdateProfileResult()

    /** 다른 사용자가 이미 쓰고 있는 닉네임이다 */
    object NicknameTaken : UpdateProfileResult()
}
