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
import kotlinx.coroutines.flow.Flow

/**
 * 테스트 공용 [CommunityRepository]. '내 활동' 삭제에 쓰는 함수만 결과를 바꿀 수 있고,
 * 나머지 함수는 호출되면 실패한다.
 */
class FakeCommunityRepository(
    var isPostExistResult: Result<Boolean> = Result.Success(true),
    var deletePostResult: Result<Unit> = Result.Success(Unit),
    var removeLikeResult: Result<Int?> = Result.Success(0),
    var removeBookmarkResult: Result<Int?> = Result.Success(0),
    var deleteActivityResult: Result<Unit> = Result.Success(Unit)
) : CommunityRepository {

    val deletedPostIds = mutableListOf<String>()
    val removedLikePostIds = mutableListOf<String>()
    val removedBookmarkPostIds = mutableListOf<String>()
    val deletedActivityIds = mutableListOf<String>()

    override suspend fun isPostExist(postId: String): Result<Boolean> = isPostExistResult

    override suspend fun deletePost(postId: String, activityId: String): Result<Unit> {
        deletedPostIds += postId
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
    override suspend fun getPostDetail(postId: String): Result<Post?> = unused()
    override suspend fun getComments(postId: String): Result<List<Comment>> = unused()
    override suspend fun getTags(): Result<List<Tag>> = unused()
    override suspend fun savePost(post: Post, activity: CommunityActivity): Result<PostCreateResult> = unused()
    override suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: Tag?): Result<SaveResult> = unused()
    override suspend fun toggleLike(postId: String, uid: String, title: String): Result<Boolean> = unused()
    override suspend fun toggleBookmark(postId: String, uid: String, title: String): Result<Boolean> = unused()
    override suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity): Result<Unit> = unused()
    override suspend fun updateComment(postId: String, commentId: String, newContent: String, activityId: String): Result<SaveResult> = unused()
    override suspend fun deleteComment(postId: String, commentId: String): Result<Boolean> = unused()
    override fun observeActivity(selected: String): Flow<List<CommunityActivity>> = unused()
    override suspend fun deleteOrphanedActivity(activityId: String): Result<Unit> = unused()
    override suspend fun findCommentItByActivityId(postId: String, activityId: String): Result<String?> = unused()

    private fun unused(): Nothing = error("Unused")
}
