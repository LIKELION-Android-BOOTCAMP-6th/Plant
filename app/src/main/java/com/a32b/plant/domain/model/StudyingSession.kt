package com.a32b.plant.domain.model

data class StudyingSession(
    val userId: String,
    val tag: String,
    val title: String,
    val potId: String,
    val time: Long,
    val log: List<String>?,
    val startedAt: Long
)
