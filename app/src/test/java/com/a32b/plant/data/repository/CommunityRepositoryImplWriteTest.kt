package com.a32b.plant.data.repository

import com.a32b.plant.data.model.CommentDto
import com.a32b.plant.data.model.CommunityActivityDto
import com.a32b.plant.data.model.PostDto
import com.a32b.plant.data.model.TagDto
import com.a32b.plant.data.source.remote.community.CommunityRemoteDataSource
import com.a32b.plant.data.source.remote.community.PostCreateWrite
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostAuthor
import com.a32b.plant.domain.model.PostCreateResult
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.result.Result
import com.a32b.plant.fake.FakeUserRepository
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.firebase.Timestamp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * 커뮤니티 쓰기(게시글 등록·수정, 댓글 수정)가 서버 응답을 제한 시간만큼만 기다리는지 확인한다 (#641).
 * Firestore 쓰기 Task 대신 [TaskCompletionSource]를 돌려주는 Fake DataSource를 쓴다.
 *
 * 서버 거부(Rejected) 경로는 여기서 확인하지 않는다. 실패 경로가 남기는 android.util.Log와
 * FirebaseFirestoreException.Code가 JVM 단위 테스트에서 동작하지 않기 때문 (기기에서 확인).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityRepositoryImplWriteTest {

    // ---------- 게시글 등록 ----------

    @Test
    fun savePost_serverAck_returnsSavedWithPostId() = runTest {
        val remote = FakeCommunityRemoteDataSource()
        remote.saveWrite.setResult(null)

        val result = repository(remote).savePost(post(), activity())

        assertEquals(Result.Success(PostCreateResult(POST_ID, SaveResult.Saved)), result)
    }

    // 오프라인처럼 응답이 없으면 실패가 아니라 Pending — 문서 ID는 쓰기 전에 정해지므로 함께 돌려준다
    @Test
    fun savePost_noResponse_returnsPendingWithPostIdAfterFiveSeconds() = runTest {
        val remote = FakeCommunityRemoteDataSource()

        val result = repository(remote).savePost(post(), activity())

        assertEquals(Result.Success(PostCreateResult(POST_ID, SaveResult.Pending)), result)
        assertEquals(5_000L, currentTime)
        // 같은 쓰기를 다시 보내지 않는다
        assertEquals(1, remote.saveCalls)
    }

    // ---------- 게시글 수정 ----------

    @Test
    fun updatePost_serverAck_returnsSavedAndUpdatesActivityTitle() = runTest {
        val remote = FakeCommunityRemoteDataSource()
        remote.updatePostWrite.setResult(null)

        val result = repository(remote).updatePost(false, POST_ID, "새 제목", "내용", Tag(id = "t"))
        runCurrent()

        assertEquals(Result.Success(SaveResult.Saved), result)
        assertEquals(listOf(POST_ID to "새 제목"), remote.activityTitleUpdates)
    }

    // Pending으로 화면을 나가도 게시글 쓰기가 나중에 반영되면 activity 제목이 이어서 바뀌어야 한다
    @Test
    fun updatePost_noResponse_returnsPendingAndUpdatesActivityTitleAfterLateAck() = runTest {
        val remote = FakeCommunityRemoteDataSource()

        val result = repository(remote).updatePost(false, POST_ID, "새 제목", "내용", Tag(id = "t"))

        assertEquals(Result.Success(SaveResult.Pending), result)
        assertEquals(5_000L, currentTime)
        runCurrent()
        assertEquals(emptyList<Pair<String, String>>(), remote.activityTitleUpdates)

        // 연결 복구 → 게시글 쓰기 반영
        remote.updatePostWrite.setResult(null)
        runCurrent()

        assertEquals(listOf(POST_ID to "새 제목"), remote.activityTitleUpdates)
        assertEquals(1, remote.updatePostCalls)
    }

    // ---------- 댓글 수정 ----------

    @Test
    fun updateComment_serverAck_returnsSaved() = runTest {
        val remote = FakeCommunityRemoteDataSource()
        remote.commentWrites += completedTask()

        val result = repository(remote).updateComment(POST_ID, COMMENT_ID, "수정", ACTIVITY_ID)

        assertEquals(Result.Success(SaveResult.Saved), result)
        assertEquals(listOf<String?>(ACTIVITY_ID), remote.commentUpdateActivityIds)
    }

    @Test
    fun updateComment_noResponse_returnsPendingWithoutRetry() = runTest {
        val remote = FakeCommunityRemoteDataSource()
        remote.commentWrites += TaskCompletionSource<Void>().task

        val result = repository(remote).updateComment(POST_ID, COMMENT_ID, "수정", ACTIVITY_ID)

        assertEquals(Result.Success(SaveResult.Pending), result)
        assertEquals(5_000L, currentTime)
        assertEquals(listOf<String?>(ACTIVITY_ID), remote.commentUpdateActivityIds)
    }

    // ---------- 준비 ----------

    // activity 제목 후속 수정이 테스트 가상 시간 안에서 돌도록 테스트 스코프를 앱 범위로 넘긴다
    // (backgroundScope 작업은 advanceUntilIdle이 기다리지 않으므로 runCurrent로 실행한다)
    private fun TestScope.repository(remote: CommunityRemoteDataSource) =
        CommunityRepositoryImpl(remote, FakeUserRepository(), backgroundScope)

    private fun post() = Post.createOriginal(
        author = PostAuthor("uid", "닉네임", "0"),
        title = "제목",
        content = "내용",
        tag = Tag(id = "t")
    )

    private fun activity() = CommunityActivity.post("uid", "제목")

    private fun completedTask(): Task<Void> = TaskCompletionSource<Void>().apply { setResult(null) }.task

    private companion object {
        const val POST_ID = "post-1"
        const val COMMENT_ID = "comment-1"
        const val ACTIVITY_ID = "activity-1"
    }
}

/** 쓰기 함수만 구현한 [CommunityRemoteDataSource]. 테스트에서 쓰지 않는 함수는 호출되면 실패한다 */
private class FakeCommunityRemoteDataSource : CommunityRemoteDataSource {
    val saveWrite = TaskCompletionSource<Void>()
    var saveCalls = 0

    val updatePostWrite = TaskCompletionSource<Void>()
    var updatePostCalls = 0
    val activityTitleUpdates = mutableListOf<Pair<String, String>>()

    /** updateComment 호출마다 앞에서부터 하나씩 돌려줄 쓰기 */
    val commentWrites = ArrayDeque<Task<Void>>()
    val commentUpdateActivityIds = mutableListOf<String?>()

    override fun savePost(post: PostDto, activity: CommunityActivityDto): PostCreateWrite {
        saveCalls++
        return PostCreateWrite(postId = "post-1", write = saveWrite.task)
    }

    override fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: TagDto?): Task<Void> {
        updatePostCalls++
        return updatePostWrite.task
    }

    override suspend fun updateActivityTitle(postId: String, title: String) {
        activityTitleUpdates += postId to title
    }

    override fun updateComment(postId: String, commentId: String, newContent: String, activityId: String?): Task<Void> {
        commentUpdateActivityIds += activityId
        return commentWrites.removeFirst()
    }

    override suspend fun isCommentExist(postId: String, commentId: String): Boolean = unused()

    override suspend fun loadPostPage(cursor: Timestamp?, pageSize: Int, tagIds: List<String>, sharedOnly: Boolean): List<PostDto> = unused()
    override suspend fun loadAllPosts(tagIds: List<String>, sharedOnly: Boolean): List<PostDto> = unused()
    override suspend fun getPostDetail(postId: String): PostDto? = unused()
    override suspend fun getComments(postId: String): List<CommentDto> = unused()
    override suspend fun getTags(): List<TagDto> = unused()
    override suspend fun deletePost(postId: String): List<String> = unused()
    override suspend fun toggleLike(postId: String, uid: String): Boolean = unused()
    override suspend fun toggleBookmark(postId: String, uid: String): Boolean = unused()
    override suspend fun removeLike(postId: String, uid: String): Boolean = unused()
    override suspend fun removeBookmark(postId: String, uid: String): Boolean = unused()
    override suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity) = unused()
    override suspend fun deleteComment(postId: String, commentId: String, deleteActivity: Boolean): Boolean = unused()
    override fun observeActivity(uid: String, selected: String): Flow<List<CommunityActivityDto>> = unused()
    override suspend fun deleteActivity(activityId: String) = unused()
    override suspend fun deleteActivityByType(uid: String, targetId: String, type: String) = unused()
    override suspend fun hasActivityByType(uid: String, targetId: String, type: String): Boolean = unused()
    override suspend fun createActivity(activity: CommunityActivity) = unused()
    override suspend fun findCommentIdByActivityId(postId: String, activityId: String): String? = unused()
    override suspend fun isPostExist(postId: String): Boolean = unused()

    private fun unused(): Nothing = throw UnsupportedOperationException("이 테스트에서 쓰지 않는 함수")
}
