package com.a32b.plant.domain.model

/** 유저 조회 결과. [isFromServer]가 false면 오프라인·시간 초과로 로컬 캐시에서 읽은 값이다. */
data class UserSnapshot(
    val user: User?,
    val isFromServer: Boolean
)
