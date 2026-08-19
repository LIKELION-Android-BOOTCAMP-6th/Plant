package com.a32b.plant.data.source.remote.community

import com.a32b.plant.data.model.CommentDto
import com.a32b.plant.data.model.CommunityActivityDto
import com.a32b.plant.data.model.PostDto
import com.a32b.plant.data.model.TagDto
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import kotlinx.coroutines.flow.Flow

interface CommunityRemoteDataSource {

    fun getPostList(): Flow<List<PostDto>>

    suspend fun getPostDetail(postId: String): PostDto?
    suspend fun getComments(postId: String): List<CommentDto>
    suspend fun getTags(): List<TagDto>

    suspend fun savePost(post: PostDto, activity: CommunityActivityDto): String
    suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: TagDto?)
    suspend fun deletePost(postId: String, activityId: String)

    suspend fun toggleLike(postId: String, uid: String, title: String): Boolean

    suspend fun addComment(postId : String, comment: Comment, activity: CommunityActivity)
    suspend fun updateComment(postId: String, commentId: String, newContent: String)
    suspend fun deleteComment(postId: String, commentId: String)

    fun observeActivity(uid: String, selected: String): Flow<List<CommunityActivityDto>>
    suspend fun deleteActivity(activityId: String)

    suspend fun findCommentIdByActivityId(postId: String, activityId: String): String?

    suspend fun isPostExist(postId: String) : Boolean
}
