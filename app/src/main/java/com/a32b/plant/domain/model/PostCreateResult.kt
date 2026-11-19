package com.a32b.plant.domain.model

/**
 * 게시글 등록 결과 (#641).
 * [postId]는 서버 응답 전에 정해지므로 [saveResult]가 [SaveResult.Pending]이어도 유효하다.
 */
data class PostCreateResult(
    val postId: String,
    val saveResult: SaveResult
)
