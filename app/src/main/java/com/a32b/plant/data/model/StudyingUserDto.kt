package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class StudyingUserDto(
    @DocumentId
    var uid: String = "",
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",
    @get:PropertyName("profileImg") @set:PropertyName("profileImg")
    var profileImg: String = "",
    @get:PropertyName("tag") @set:PropertyName("tag")
    var tag: String = "",
    @get:PropertyName("studyingTime") @set:PropertyName("studyingTime")
    var studyingTime: Long = 0L,
    // 마지막 갱신 서버 시각 — 쓸 때는 RemoteDataSource가 serverTimestamp()로 채운다 (#525)
    // 예약 함수(functions/index.js cleanUpStaleStudying)가 이 값이 오래된 문서를 지운다
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Timestamp? = null
)
