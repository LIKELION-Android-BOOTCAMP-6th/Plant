package com.a32b.plant.data.model

import com.google.firebase.firestore.PropertyName

// Post 문서에 임베드된 studyLogs 배열 전용 DTO
// StudyLog 도메인 모델이 직접 직렬화된 결과물이므로 createAt/startedAt이 Long으로 저장됨
// StudyLogDto(컬렉션용, Timestamp 포맷)와 구분하여 별도 운용
data class StudyLogEmbedDto(
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("contents") @set:PropertyName("contents")
    var contents: List<String> = emptyList(),
    @get:PropertyName("completedContents") @set:PropertyName("completedContents")
    var completedContents: List<Boolean> = emptyList(),
    @get:PropertyName("studyingTime") @set:PropertyName("studyingTime")
    var studyingTime: Long = 0L,
    @get:PropertyName("createAt") @set:PropertyName("createAt")
    var createAt: Long? = null,
    @get:PropertyName("startedAt") @set:PropertyName("startedAt")
    var startedAt: Long = 0L,
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("selected") @set:PropertyName("selected")
    var isSelected: Boolean = false
)
