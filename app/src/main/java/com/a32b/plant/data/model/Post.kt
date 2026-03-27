package com.a32b.plant.data.model

import com.google.firebase.firestore.IgnoreExtraProperties

@IgnoreExtraProperties
data class Post(
    val id: String = "",
    val title: String = "",
    val nickName: String = "",
    val content: String = "",
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: String = "",
    val comments: List<Map<String, Any>> = emptyList()
)