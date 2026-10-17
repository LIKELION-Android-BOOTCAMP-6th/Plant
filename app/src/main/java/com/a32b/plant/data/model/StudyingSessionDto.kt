package com.a32b.plant.data.model

data class StudyingSessionDto(
    var userId: String = "",
    var tag: String = "",
    var title: String = "",
    var potId: String = "",
    var time: Long = 0L,
    var log: List<String>? = null,
    var startedAt: Long = 0L
)
