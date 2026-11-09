package com.a32b.plant.data.model

data class StudyingSessionDto(
    var userId: String = "",
    var tag: String = "",
    var title: String = "",
    var potId: String = "",
    var time: Long = 0L,
    var log: List<String>? = null,
    var startedAt: Long = 0L,
    var isPaused: Boolean = false,
    var status: String? = null, // 이전 버전 백업에는 값이 없음
    var sessionId: String? = null, // 이전 버전 백업에는 값이 없음 (#453)
    var completed: List<Boolean>? = null // 이전 버전 백업에는 값이 없음 (#453)
)
