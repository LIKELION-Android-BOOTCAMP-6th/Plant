package com.a32b.plant.data.model

import com.google.firebase.firestore.PropertyName

data class StudyingUserDto(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",
    @get:PropertyName("profileImg") @set:PropertyName("profileImg")
    var profileImg: String = "",
    @get:PropertyName("tag") @set:PropertyName("tag")
    var tag: String = "",
    @get:PropertyName("studyingTime") @set:PropertyName("studyingTime")
    var studyingTime: Long = 0L
)