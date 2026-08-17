package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {

    // 글 목록은 실시간 구독 (다른 사용자의 활동이 바로 반영되는 게 UX상 이득이 큼)
    fun getPostList(): Flow<List<Post>>

    // 상세/댓글은 단건 조회 (pull-to-refresh로 갱신, read 비용/복잡도 절감)
    suspend fun getPostDetail(postId: String): Result<Post?>
    suspend fun getComments(postId: String): Result<List<Comment>>

    suspend fun getTags(): Result<List<Tag>>

    suspend fun savePost(post: Post, activity: CommunityActivity): Result<String>
    suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: Tag?): Result<Unit>
    suspend fun deletePost(postId: String, activityId: String): Result<Unit>

    suspend fun toggleLike(postId: String, uid: String, title: String): Result<Boolean>

    suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity): Result<Unit>
    suspend fun updateComment(postId: String, commentId: String, newContent: String): Result<Unit>
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>

    fun observeActivity(selected: String): Flow<List<CommunityActivity>>
    suspend fun deleteActivity(activityId : String): Result<Unit>
    suspend fun isPostExist(postId: String) : Result<Boolean>
}
