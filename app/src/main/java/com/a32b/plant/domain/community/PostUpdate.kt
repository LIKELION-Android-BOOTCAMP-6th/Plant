package com.a32b.plant.domain.community

import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.Tag

// 상세 화면에서의 변경을 목록 화면에 즉시 반영하기 위한 부분 패치. null 필드는 "변경 없음"을 의미
data class PostUpdate(
    val postId: String,
    val isLiked: Boolean? = null,
    val likeCount: Int? = null,
    val isBookmarked: Boolean? = null,
    val bookmarkCount: Int? = null,
    val commentCount: Int? = null,
    val title: String? = null,
    val content: String? = null,
    val tag: Tag? = null,
    val updatedAt: Long? = null,
    val reportCount: Int? = null,
    val isHidden: Boolean? = null
)

fun Post.applyUpdate(update: PostUpdate): Post {
    if (postId != update.postId) return this
    return copy(
        isLiked = update.isLiked ?: isLiked,
        likeCount = update.likeCount ?: likeCount,
        isBookmarked = update.isBookmarked ?: isBookmarked,
        bookmarkCount = update.bookmarkCount ?: bookmarkCount,
        commentCount = update.commentCount ?: commentCount,
        title = update.title ?: title,
        content = update.content ?: content,
        tag = update.tag ?: tag,
        updatedAt = update.updatedAt ?: updatedAt,
        reportCount = update.reportCount ?: reportCount,
        isHidden = update.isHidden ?: isHidden
    )
}
