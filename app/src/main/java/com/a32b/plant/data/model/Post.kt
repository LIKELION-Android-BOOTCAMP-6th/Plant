package com.a32b.plant.data.model

import com.google.firebase.Timestamp

data class Post(
    val postId : String = "",
    val authorId: String = "",
    val authorNickname: String = "",
    val authorProfileImg: String = "",
    val title: String = "",
    val content: String = "",
    val tag: List<String> = emptyList(),
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val createdAt: Timestamp? = null,
    val activityId: String = ""
)

data class Comment(
    val userId: String = "",
    val userNickname: String = "",
    val userProfileImg: String = "",
    val content: String = "",
    val createdAt: Timestamp? = null,
    val activityId: String = ""
)

data class PostLike(
    val isLiked: Boolean = false,
    val activityId: String = ""
)


