package com.a32b.plant.data.source.remote.community

import com.a32b.plant.data.model.CommentDto
import com.a32b.plant.data.model.CommunityActivityDto
import com.a32b.plant.data.model.PostDto
import com.a32b.plant.data.model.TagDto
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

/** 게시글 등록 쓰기 — 문서 ID는 쓰기 전에 정해지므로 서버 응답과 상관없이 바로 알 수 있다 (#641) */
data class PostCreateWrite(val postId: String, val write: Task<Void>)

interface CommunityRemoteDataSource {

    suspend fun loadPostPage(
        cursor: Timestamp?,
        pageSize: Int,
        tagIds: List<String>,
        sharedOnly: Boolean
    ): List<PostDto>

    // 검색용: limit 없이 전체 로드 (클라이언트 contains 필터 전용)
    suspend fun loadAllPosts(
        tagIds: List<String>,
        sharedOnly: Boolean
    ): List<PostDto>

    suspend fun getPostDetail(postId: String): PostDto?
    suspend fun getComments(postId: String): List<CommentDto>
    suspend fun getTags(): List<TagDto>

    // 등록·수정 쓰기는 기다리지 않고 Task를 그대로 돌려준다 (기다리는 시간은 Repository가 정함, #641)
    fun savePost(post: PostDto, activity: CommunityActivityDto): PostCreateWrite
    fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: TagDto?): Task<Void>
    /** 게시글 activity의 제목을 게시글 문서의 현재 제목으로 맞춘다. [title]은 문서에서 제목을 읽지 못했을 때만 쓴다 */
    suspend fun updateActivityTitle(postId: String, title: String)
    suspend fun deletePost(postId: String): List<String>

    suspend fun toggleLike(postId: String, uid: String): Boolean
    suspend fun toggleBookmark(postId: String, uid: String): Boolean
    /**
     * 좋아요/북마크 "해제" 전용 — 토글과 달리 없으면 추가하지 않아 여러 번 호출해도 결과가 같음
     * @return 이번 호출이 실제로 해제했으면 true, 이미 해제된 상태(또는 게시글 없음)라 쓰기를 하지 않았으면 false
     */
    suspend fun removeLike(postId: String, uid: String): Boolean
    suspend fun removeBookmark(postId: String, uid: String): Boolean

    suspend fun addComment(postId : String, comment: Comment, activity: CommunityActivity)
    /** activityId가 있으면 댓글 본문과 activity 문구를 batch 한 번으로 함께 수정, null이면 본문만 수정. 기다리지 않고 Task를 돌려준다 */
    fun updateComment(postId: String, commentId: String, newContent: String, activityId: String?): Task<Void>
    /**
     * 댓글 삭제 + 댓글 수 감소를 하나의 트랜잭션으로 처리. deleteActivity가 true면 연결된 activity 삭제도 같은 트랜잭션에 포함
     * @return 이번 호출이 실제로 댓글을 지웠으면 true, 이미 없던 댓글이면 false
     */
    suspend fun deleteComment(postId: String, commentId: String, deleteActivity: Boolean): Boolean

    fun observeActivity(uid: String, selected: String): Flow<List<CommunityActivityDto>>
    suspend fun deleteActivity(activityId: String)
    suspend fun isCommentExist(postId: String, commentId: String): Boolean
    suspend fun deleteActivityByType(uid: String, targetId: String, type: String)
    suspend fun hasActivityByType(uid: String, targetId: String, type: String): Boolean
    suspend fun createActivity(activity: CommunityActivity)

    suspend fun findCommentIdByActivityId(postId: String, activityId: String): String?

    suspend fun isPostExist(postId: String) : Boolean
}
