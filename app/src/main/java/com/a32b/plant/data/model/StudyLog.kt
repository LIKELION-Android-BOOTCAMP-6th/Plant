package com.a32b.plant.data.model

import com.google.firebase.Timestamp

data class StudyLog(
    val title: String,
    val contents: List<String>,
    val studyingTime: Long,
    val createAt: Timestamp = Timestamp.now()
)
