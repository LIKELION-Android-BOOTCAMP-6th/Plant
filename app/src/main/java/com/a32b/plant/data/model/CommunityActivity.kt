package com.a32b.plant.data.model

import com.a32b.plant.data.di.CurrentUser
import com.google.firebase.Timestamp

data class CommunityActivity(
    val uid: String = CurrentUser.uid,
    val type: String = "",
    val title: String = "",
    val targetId: String = "",
    val comment: String? = null,
    val commentId: String? = null, //코멘트는 코멘트용 아이디가 따로 있어야 됨 타겟 아이디에 코멘트 아이디를 저장하면 안 넘어감
    val createAt: Timestamp = Timestamp.now()
)
