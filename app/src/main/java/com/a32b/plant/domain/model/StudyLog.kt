package com.a32b.plant.domain.model


data class StudyLog(
    val title: String,
    val contents: List<String>,
    val studyingTime: Long,
    val createAt: Long?,
    val id: String,
    val isSelected: Boolean
    )
