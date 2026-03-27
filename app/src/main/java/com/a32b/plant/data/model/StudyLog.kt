package com.a32b.plant.data.model

import com.google.firebase.Timestamp

data class StudyLog(
    val title: String = "",
    val contents: List<String> = emptyList(),
    val studyingTime: Long = 0L,
    val createAt: Timestamp = Timestamp.now(),
    val id: String = "",
    )
