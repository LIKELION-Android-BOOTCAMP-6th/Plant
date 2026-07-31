package com.a32b.plant.data.model

data class StudyingSession(
    val userId: String,
    val tag: String,
    val title: String,
    val potId: String,
    val time: Long,
    val log: List<String>? = null
)
