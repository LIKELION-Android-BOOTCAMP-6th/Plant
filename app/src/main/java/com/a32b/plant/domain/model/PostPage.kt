package com.a32b.plant.domain.model

data class PostPage(
    val items: List<Post>,
    val nextCursor: Long?,   // epoch millis. null이면 더 이상 없음
    val hasMore: Boolean
)
