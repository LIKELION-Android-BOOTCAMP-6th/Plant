package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostPage
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.result.Result

interface CommunityRepository {

    // 글 목록은 커서 기반 페이지네이션 (단건 조회 + pull-to-refresh로 정책 통일)
    suspend fun loadPostPage(
        cursor: Long?,
        pageSize: Int,
        tagIds: List<String>,
        sharedOnly: Boolean
    ): Result<PostPage>

    // 검색용: 전체 로드 (클라이언트 contains 필터 전용)
    suspend fun getAllPosts(
        tagIds: List<String>,
        sharedOnly: Boolean
    ): Result<List<Post>>

    // 상세/댓글은 단건 조회 (pull-to-refresh로 갱신, read 비용/복잡도 절감)
    suspend fun getPostDetail(postId: String): Result<Post?>
    suspend fun getComments(postId: String): Result<List<Comment>>

    suspend fun getTags(): Result<List<Tag>>

    suspend fun savePost(post: Post, activity: CommunityActivity): Result<String>
    suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: Tag?): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>

    suspend fun toggleLike(postId: String, uid: String, title: String): Result<Boolean>

    suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity): Result<Unit>
    suspend fun updateComment(postId: String, commentId: String, newContent: String): Result<Unit>
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
}
