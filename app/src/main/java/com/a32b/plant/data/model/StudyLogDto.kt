package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName

data class StudyLogDto(
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("contents") @set:PropertyName("contents")
    var contents: List<String> = emptyList(),
    @get:PropertyName("studyingTime") @set:PropertyName("studyingTime")
    var studyingTime: Long = 0L,
    @get:PropertyName("createAt") @set:PropertyName("createAt")
    var createAt: Timestamp = Timestamp.now(),
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = ""
)
