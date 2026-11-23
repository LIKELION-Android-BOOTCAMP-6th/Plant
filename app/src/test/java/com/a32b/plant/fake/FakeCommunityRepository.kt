package com.a32b.plant.fake

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostCreateResult
import com.a32b.plant.domain.model.PostPage
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/**
 * 테스트 공용 [CommunityRepository]. '내 활동'의 조회와 게시글·좋아요·북마크 삭제에 쓰는 함수만 결과를 바꿀 수 있고,
 * 나머지 함수(내 활동의 댓글 삭제에 쓰는 함수 포함)는 호출되면 실패한다.
 * 게시글 상세·댓글 조회는 [postDetailResult]·[commentsResult]를 넣었을 때만 쓸 수 있다 (넣지 않으면 호출 시 실패).
 * [deletePostGate]를 넣으면 게시글 삭제는 게이트가 완료될 때까지 끝나지 않는다 (응답 대기 재현).
 * [pendingActivityTypes]에 넣은 유형은 내 활동 목록을 구독해도 목록이 오지 않는다 (탭 로딩 대기 재현).
 */
class FakeCommunityRepository(
    var isPostExistResult: Result<Boolean> = Result.Success(true),
    var deletePostResult: Result<Unit> = Result.Success(Unit),
    var removeLikeResult: Result<Int?> = Result.Success(0),
    var removeBookmarkResult: Result<Int?> = Result.Success(0),
    var deleteActivityResult: Result<Unit> = Result.Success(Unit),
    var deletePostGate: CompletableDeferred<Unit>? = null,
    var postDetailResult: Result<Post?>? = null,
    var commentsResult: Result<List<Comment>>? = null,
    val activities: MutableStateFlow<List<CommunityActivity>> = MutableStateFlow(emptyList()),
    var pendingActivityTypes: Set<String> = emptySet()
) : CommunityRepository {

    val deletedPostIds = mutableListOf<String>()
    val removedLikePostIds = mutableListOf<String>()
    val removedBookmarkPostIds = mutableListOf<String>()
    val deletedActivityIds = mutableListOf<String>()

    override suspend fun isPostExist(postId: String): Result<Boolean> = isPostExistResult

    override suspend fun deletePost(postId: String, activityId: String): Result<Unit> {
        deletedPostIds += postId
        deletePostGate?.await()
        return deletePostResult
    }

    override suspend fun removeLike(postId: String, uid: String): Result<Int?> {
        removedLikePostIds += postId
        return removeLikeResult
    }

    override suspend fun removeBookmark(postId: String, uid: String): Result<Int?> {
        removedBookmarkPostIds += postId
        return removeBookmarkResult
    }

    override suspend fun deleteActivity(activityId: String): Result<Unit> {
        deletedActivityIds += activityId
        return deleteActivityResult
    }

    override suspend fun loadPostPage(cursor: Long?, pageSize: Int, tagIds: List<String>, sharedOnly: Boolean): Result<PostPage> = unused()
    override suspend fun getAllPosts(tagIds: List<String>, sharedOnly: Boolean): Result<List<Post>> = unused()
    override suspend fun getPostDetail(postId: String): Result<Post?> = postDetailResult ?: unused()
    override suspend fun getComments(postId: String): Result<List<Comment>> = commentsResult ?: unused()
    override suspend fun getTags(): Result<List<Tag>> = unused()
    override suspend fun savePost(post: Post, activity: CommunityActivity): Result<PostCreateResult> = unused()
    override suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: Tag?): Result<SaveResult> = unused()
    override suspend fun toggleLike(postId: String, uid: String, title: String): Result<Boolean> = unused()
    override suspend fun toggleBookmark(postId: String, uid: String, title: String): Result<Boolean> = unused()
    override suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity): Result<Unit> = unused()
    override suspend fun updateComment(postId: String, commentId: String, newContent: String, activityId: String): Result<SaveResult> = unused()
    override suspend fun deleteComment(postId: String, commentId: String): Result<Boolean> = unused()
    override fun observeActivity(selected: String): Flow<List<CommunityActivity>> =
        if (selected in pendingActivityTypes) flow { awaitCancellation() }
        else activities.map { list -> list.filter { it.type == selected } }
    override suspend fun deleteOrphanedActivity(activityId: String): Result<Unit> = unused()
    override suspend fun findCommentItByActivityId(postId: String, activityId: String): Result<String?> = unused()

    private fun unused(): Nothing = error("Unused")
}
