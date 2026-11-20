package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.community.PostUpdate
import com.a32b.plant.domain.community.PostUpdateNotifier
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeCommunityRepository
import com.a32b.plant.fake.RecordingPostUpdateNotifier
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * '내 활동' 삭제 결과가 커뮤니티 목록에 신호로 전달되는지 확인한다 (#712).
 * 성공한 묶음만 알리고, 실패한 묶음은 알리지 않아야 목록이 서버와 다른 상태로 바뀌지 않는다.
 */
class DeleteActivitiesUseCaseTest {

    // ---------- 게시글 ----------

    @Test
    fun `게시글 삭제 성공 시 목록에 삭제를 알린다`() = runBlocking {
        val repository = FakeCommunityRepository()
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(listOf(activity("a1", ActivityType.POST, "post-1")))

        assertTrue(result is Result.Success)
        assertEquals(listOf("post-1"), notifier.deleted)
        assertTrue(notifier.updates.isEmpty())
    }

    @Test
    fun `게시글 삭제 실패 시 목록에 알리지 않는다`() = runBlocking {
        val repository = FakeCommunityRepository(deletePostResult = Result.Failure(AppError.Network()))
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(listOf(activity("a1", ActivityType.POST, "post-1")))

        assertTrue(result is Result.Failure)
        assertTrue(notifier.deleted.isEmpty())
        assertTrue(notifier.updates.isEmpty())
    }

    // 서버(Source.SERVER) 조회로 없다고 확인된 글 — 목록에 남아 있으면 빼야 한다
    @Test
    fun `게시글이 이미 없으면 activity를 정리하고 목록에 삭제를 알린다`() = runBlocking {
        val repository = FakeCommunityRepository(isPostExistResult = Result.Success(false))
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(listOf(activity("a1", ActivityType.LIKE, "post-1")))

        assertTrue(result is Result.Success)
        assertEquals(listOf("a1"), repository.deletedActivityIds)
        assertTrue(repository.removedLikePostIds.isEmpty())
        assertEquals(listOf("post-1"), notifier.deleted)
        assertTrue(notifier.updates.isEmpty())
    }

    @Test
    fun `게시글 존재 조회 실패 시 목록에 알리지 않는다`() = runBlocking {
        val repository = FakeCommunityRepository(isPostExistResult = Result.Failure(AppError.Network()))
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(listOf(activity("a1", ActivityType.POST, "post-1")))

        assertTrue(result is Result.Failure)
        assertTrue(notifier.deleted.isEmpty())
        assertTrue(notifier.updates.isEmpty())
    }

    // ---------- 좋아요·북마크 ----------

    @Test
    fun `좋아요 해제 성공 시 해제 상태와 해제 후 좋아요 수를 알린다`() = runBlocking {
        val repository = FakeCommunityRepository(removeLikeResult = Result.Success(4))
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(listOf(activity("a1", ActivityType.LIKE, "post-1")))

        assertTrue(result is Result.Success)
        assertEquals(listOf(PostUpdate(postId = "post-1", isLiked = false, likeCount = 4)), notifier.updates)
        assertTrue(notifier.deleted.isEmpty())
    }

    @Test
    fun `북마크 해제 성공 시 해제 상태와 해제 후 북마크 수를 알린다`() = runBlocking {
        val repository = FakeCommunityRepository(removeBookmarkResult = Result.Success(2))
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(listOf(activity("a1", ActivityType.BOOKMARK, "post-1")))

        assertTrue(result is Result.Success)
        assertEquals(listOf(PostUpdate(postId = "post-1", isBookmarked = false, bookmarkCount = 2)), notifier.updates)
        assertTrue(notifier.deleted.isEmpty())
    }

    @Test
    fun `좋아요 해제 실패 시 목록에 알리지 않는다`() = runBlocking {
        val repository = FakeCommunityRepository(removeLikeResult = Result.Failure(AppError.Network()))
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(listOf(activity("a1", ActivityType.LIKE, "post-1")))

        assertTrue(result is Result.Failure)
        assertTrue(notifier.updates.isEmpty())
        assertTrue(notifier.deleted.isEmpty())
    }

    // 해제 트랜잭션 시점에 게시글 문서가 없던 경우 (존재 확인과 해제 사이에 삭제됨)
    @Test
    fun `좋아요 해제 후 카운트가 null이면 목록에 삭제를 알린다`() = runBlocking {
        val repository = FakeCommunityRepository(removeLikeResult = Result.Success(null))
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(listOf(activity("a1", ActivityType.LIKE, "post-1")))

        assertTrue(result is Result.Success)
        assertEquals(listOf("post-1"), notifier.deleted)
        assertTrue(notifier.updates.isEmpty())
    }

    @Test
    fun `북마크 해제 후 카운트가 null이면 목록에 삭제를 알린다`() = runBlocking {
        val repository = FakeCommunityRepository(removeBookmarkResult = Result.Success(null))
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(listOf(activity("a1", ActivityType.BOOKMARK, "post-1")))

        assertTrue(result is Result.Success)
        assertEquals(listOf("post-1"), notifier.deleted)
        assertTrue(notifier.updates.isEmpty())
    }

    @Test
    fun `같은 글의 좋아요 activity가 여러 개여도 한 번만 해제하고 한 번만 알린다`() = runBlocking {
        val repository = FakeCommunityRepository(removeLikeResult = Result.Success(0))
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(
            listOf(activity("a1", ActivityType.LIKE, "post-1"), activity("a2", ActivityType.LIKE, "post-1"))
        )

        assertTrue(result is Result.Success)
        assertEquals(listOf("post-1"), repository.removedLikePostIds)
        assertEquals(listOf(PostUpdate(postId = "post-1", isLiked = false, likeCount = 0)), notifier.updates)
    }

    @Test
    fun `세션이 만료되면 해제하지 않고 목록에 알리지 않는다`() = runBlocking {
        val repository = FakeCommunityRepository()
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier, user = null)(listOf(activity("a1", ActivityType.LIKE, "post-1")))

        assertTrue(result is Result.Failure && result.error is AppError.UnknownUser)
        assertTrue(repository.removedLikePostIds.isEmpty())
        assertTrue(notifier.updates.isEmpty())
        assertTrue(notifier.deleted.isEmpty())
    }

    // ---------- 묶음별 독립 ----------

    @Test
    fun `일부만 실패하면 성공한 묶음만 알린다`() = runBlocking {
        val repository = FakeCommunityRepository(
            deletePostResult = Result.Success(Unit),
            removeBookmarkResult = Result.Failure(AppError.Network())
        )
        val notifier = RecordingPostUpdateNotifier()

        val result = useCase(repository, notifier)(
            listOf(activity("a1", ActivityType.POST, "post-1"), activity("a2", ActivityType.BOOKMARK, "post-2"))
        )

        assertTrue(result is Result.Failure)
        assertEquals(listOf("post-1"), notifier.deleted)
        assertTrue(notifier.updates.isEmpty())
    }

    // ---------- 도우미 ----------

    private fun useCase(
        repository: FakeCommunityRepository,
        notifier: PostUpdateNotifier,
        user: User? = User.create().copy(uid = "uid-1")
    ) = DeleteActivitiesUseCase(
        repository = repository,
        ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
            FakeUserRepository(user),
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
            }
        ),
        postUpdateNotifier = notifier
    )

    private fun activity(id: String, type: String, targetId: String) = CommunityActivity(
        id = id,
        uid = "uid-1",
        type = type,
        title = "제목",
        targetId = targetId,
        comment = null,
        commentId = null,
        createAt = null
    )
}
