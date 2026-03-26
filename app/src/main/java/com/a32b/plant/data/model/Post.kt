package com.a32b.plant.data.model



data class Post(
    val id: String = "",
    val nickName: String = "",
    val content: String = "",
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val isLiked: Boolean = false,
    val createdAt: String = ""
)