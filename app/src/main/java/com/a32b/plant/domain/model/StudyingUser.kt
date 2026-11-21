package com.a32b.plant.domain.model

data class StudyingUser(
    val uid: String,
    val nickname: String,
    val profileImg: String,
    val tag: String,
    val studyingTime: Long,
    // 마지막 갱신 서버 시각(ms) — 읽을 때만 채워진다. 없으면(필드 없는 예전 문서) null (#618)
    // 쓸 때는 RemoteDataSource가 서버 시각을 넣으므로 이 값은 쓰지 않는다
    val updatedAt: Long? = null
)
