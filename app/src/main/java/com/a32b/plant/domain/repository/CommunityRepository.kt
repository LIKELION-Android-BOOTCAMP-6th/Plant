package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface CommunityRepository {

    // 글 목록/상세/댓글 실시간 구독
    fun getPostList(): Flow<List<Post>>
    fun getPostDetail(postId: String): Flow<Post?>
    fun observeComments(postId: String): Flow<List<Comment>>

    suspend fun getTags(): Result<List<Tag>>

    suspend fun savePost(post: Post, activity: CommunityActivity): Result<String>
    suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: Tag?): Result<Unit>
    suspend fun deletePost(postId: String): Result<Unit>

    suspend fun toggleLike(postId: String, uid: String, isAlreadyLiked: Boolean, title: String): Result<Unit>

    suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity): Result<Unit>
    suspend fun updateComment(postId: String, commentId: String, newContent: String): Result<Unit>
    suspend fun deleteComment(postId: String, commentId: String): Result<Unit>
}
