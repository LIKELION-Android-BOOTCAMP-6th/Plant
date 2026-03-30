package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class StudyLog(
    val title: String = "",
    val contents: List<String> = emptyList(),
    val studyingTime: Long = 0L,
    val createAt: Timestamp = Timestamp.now(),
    val id: String = "",

    //사용자 선택 필드
    @get:Exclude
    val isSelected: Boolean = false
    )
