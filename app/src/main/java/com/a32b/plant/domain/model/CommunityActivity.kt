package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ActivityType

data class CommunityActivity(
    val id: String,
    val uid: String,
    val type: String ,
    val title: String,
    val targetId: String,
    val comment: String?,
    val commentId: String?, //코멘트는 코멘트용 아이디가 따로 있어야 됨 타겟 아이디에 코멘트 아이디를 저장하면 안 넘어감
    val createAt: Long?
){
    companion object {
        fun like(uid: String, title: String, targetId: String) = CommunityActivity(
            id = "",
            uid = uid,
            type = ActivityType.LIKE,
            title = title,
            targetId = targetId,
            comment = null,
            commentId = null,
            createAt = null
        )

        fun comment(uid: String, title: String, targetId: String, content:String, commentId: String) = CommunityActivity(
            id = "",
            uid = uid,
            type = ActivityType.COMMENT,
            title = title,
            targetId = targetId,
            comment = content,
            commentId = commentId,
            createAt = null
        )

        fun post(uid: String, title: String) = CommunityActivity(
            id = "",
            uid = uid,
            type = ActivityType.POST,
            title = title,
            targetId = "",
            comment = null,
            commentId = null,
            createAt = null
        )
    }
}
