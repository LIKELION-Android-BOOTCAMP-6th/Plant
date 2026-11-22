package com.a32b.plant.presentation.community.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommentUser
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostAuthor
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.network.NetworkMonitor
import com.a32b.plant.domain.repository.CommunityReportRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.ReportReason
import com.a32b.plant.domain.usecase.community.AddCommentUseCase
import com.a32b.plant.domain.usecase.community.BlockUserUseCase
import com.a32b.plant.domain.usecase.community.CreateCommunityReportUseCase
import com.a32b.plant.domain.usecase.community.ObserveBlockedUsersUseCase
import com.a32b.plant.domain.usecase.community.ToggleBookmarkUseCase
import com.a32b.plant.domain.usecase.community.ToggleLikeUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeBlockRepository
import com.a32b.plant.fake.FakeCommunityRepository
import com.a32b.plant.fake.FakeUserRepository
import com.a32b.plant.fake.RecordingPostUpdateNotifier
import com.a32b.plant.presentation.community.CommunityConstants
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 게시글 상세의 사용자 차단 (#660) */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityDetailViewModelBlockTest {

    @Test
    fun `차단한 사용자의 댓글은 화면 목록에서만 빠지고 전체 목록은 유지한다`() = runDetailTest(
        initiallyBlocked = listOf(blocked("bad"))
    ) { _, viewModel, _ ->
        assertEquals(listOf("c-ok", "c-mine"), viewModel.visibleComments.value.map { it.commentId })
        assertEquals(3, viewModel.uiState.value.commentList.size)
        assertFalse(viewModel.isPostAuthorBlocked.value)
    }

    @Test
    fun `댓글 작성자를 차단하면 그 사용자 댓글이 사라지고 화면에 남는다`() = runDetailTest { blockRepository, viewModel, events ->
        viewModel.openCommentAuthorBlockDialog(viewModel.visibleComments.value.first { it.user.uid == "bad" })
        assertEquals("bad", viewModel.uiState.value.blockTarget?.uid)

        viewModel.confirmBlock()
        runCurrent()

        assertEquals(listOf(ME to "bad"), blockRepository.blockCalls.map { it.first to it.second.uid })
        assertEquals("나쁜", blockRepository.blockCalls.single().second.nickname)
        assertFalse(viewModel.visibleComments.value.any { it.user.uid == "bad" })
        assertNull(viewModel.uiState.value.blockTarget)
        assertFalse(viewModel.uiState.value.isBlockSubmitting)
        assertEquals(listOf<CommunityDetailEvent>(CommunityDetailEvent.ShowToast("차단했어요")), events)
    }

    @Test
    fun `게시글 작성자를 차단하면 목록으로 돌아간다`() = runDetailTest { blockRepository, viewModel, events ->
        viewModel.openPostAuthorBlockDialog()
        viewModel.confirmBlock()
        runCurrent()

        assertEquals(AUTHOR, blockRepository.blockCalls.single().second.uid)
        assertTrue(viewModel.isPostAuthorBlocked.value)
        assertEquals(
            listOf(CommunityDetailEvent.ShowToast("차단했어요"), CommunityDetailEvent.NavigateBack),
            events
        )
    }

    @Test
    fun `오프라인 등으로 서버 응답이 없어도 바로 숨기고 대기 안내를 띄운다`() = runDetailTest(
        blockResult = Result.Success(SaveResult.Pending)
    ) { _, viewModel, events ->
        viewModel.openCommentAuthorBlockDialog(viewModel.visibleComments.value.first { it.user.uid == "bad" })
        viewModel.confirmBlock()
        runCurrent()

        assertFalse(viewModel.visibleComments.value.any { it.user.uid == "bad" })
        assertEquals(listOf<CommunityDetailEvent>(CommunityDetailEvent.ShowToast(CommunityConstants.WRITE_PENDING_MESSAGE)), events)
    }

    @Test
    fun `차단이 실패하면 다이얼로그를 유지하고 안내한다`() = runDetailTest(
        blockResult = Result.Failure(com.a32b.plant.domain.error.AppError.Custom("차단에 실패했어요."))
    ) { _, viewModel, events ->
        viewModel.openPostAuthorBlockDialog()
        viewModel.confirmBlock()
        runCurrent()

        assertEquals(AUTHOR, viewModel.uiState.value.blockTarget?.uid)
        assertFalse(viewModel.uiState.value.isBlockSubmitting)
        assertEquals(listOf<CommunityDetailEvent>(CommunityDetailEvent.ShowToast("차단에 실패했어요.")), events)
    }

    @Test
    fun `내 댓글과 서버 반영 전 임시 댓글은 차단 창을 열지 않는다`() = runDetailTest { blockRepository, viewModel, _ ->
        viewModel.openCommentAuthorBlockDialog(viewModel.visibleComments.value.first { it.user.uid == ME })
        assertNull(viewModel.uiState.value.blockTarget)

        viewModel.openCommentAuthorBlockDialog(
            Comment.createPending(CommentUser("other", "다른", "1"), "등록 중", 0L)
        )
        assertNull(viewModel.uiState.value.blockTarget)
        assertTrue(blockRepository.blockCalls.isEmpty())
    }

    @Test
    fun `내 글에서는 게시글 작성자 차단 창을 열지 않는다`() = runDetailTest(postAuthor = ME) { _, viewModel, _ ->
        viewModel.openPostAuthorBlockDialog()
        assertNull(viewModel.uiState.value.blockTarget)
    }

    @Test
    fun `차단한 사용자의 글로 직접 들어오면 차단 안내 상태가 된다`() = runDetailTest(
        initiallyBlocked = listOf(blocked(AUTHOR))
    ) { _, viewModel, _ ->
        assertTrue(viewModel.isPostAuthorBlocked.value)
    }

    @Test
    fun `차단을 해제하면 숨겼던 글과 댓글이 다시 보인다`() = runDetailTest(
        initiallyBlocked = listOf(blocked(AUTHOR), blocked("bad"))
    ) { blockRepository, viewModel, _ ->
        assertTrue(viewModel.isPostAuthorBlocked.value)
        blockRepository.blockedOf(ME).value = emptyList()
        runCurrent()

        assertFalse(viewModel.isPostAuthorBlocked.value)
        assertEquals(3, viewModel.visibleComments.value.size)
    }

    // ---------- 도우미 ----------

    private fun runDetailTest(
        postAuthor: String = AUTHOR,
        initiallyBlocked: List<BlockedUser> = emptyList(),
        blockResult: Result<SaveResult> = Result.Success(SaveResult.Saved),
        block: suspend TestScope.(FakeBlockRepository, CommunityDetailViewModel, List<CommunityDetailEvent>) -> Unit
    ) = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val userRepository = FakeUserRepository(User.create().copy(uid = ME, nickname = "나", profileImg = "1"))
            val ensureCurrentUser = EnsureCurrentUserUseCase(
                userRepository,
                object : SessionExpiredNotifier {
                    override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
                }
            )
            val communityRepository = FakeCommunityRepository(
                postDetailResult = Result.Success(post(postAuthor)),
                commentsResult = Result.Success(
                    listOf(comment("c-bad", "bad", "나쁜"), comment("c-ok", "ok", "좋은"), comment("c-mine", ME, "나"))
                )
            )
            val blockRepository = FakeBlockRepository(blockResult = blockResult)
            blockRepository.blockedOf(ME).value = initiallyBlocked

            val viewModel = CommunityDetailViewModel(
                repository = communityRepository,
                addCommentUseCase = AddCommentUseCase(communityRepository, ensureCurrentUser),
                toggleLikeUseCase = ToggleLikeUseCase(communityRepository, ensureCurrentUser),
                toggleBookmarkUseCase = ToggleBookmarkUseCase(communityRepository, ensureCurrentUser),
                createCommunityReportUseCase = CreateCommunityReportUseCase(UnusedReportRepository, ensureCurrentUser),
                blockUserUseCase = BlockUserUseCase(blockRepository, ensureCurrentUser),
                observeBlockedUsersUseCase = ObserveBlockedUsersUseCase(blockRepository, userRepository),
                ensureCurrentUserUseCase = ensureCurrentUser,
                postUpdateNotifier = RecordingPostUpdateNotifier(),
                networkMonitor = OnlineNetworkMonitor,
                savedStateHandle = SavedStateHandle(mapOf("postId" to POST_ID))
            )
            val events = mutableListOf<CommunityDetailEvent>()
            val collectors = launch(dispatcher) {
                launch { viewModel.event.collect { events += it } }
                // WhileSubscribed 상태는 화면처럼 구독해야 값이 갱신된다
                launch { viewModel.visibleComments.collect {} }
                launch { viewModel.isPostAuthorBlocked.collect {} }
            }
            advanceUntilIdle()   // loadInitial (차단 목록 대기 포함)
            events.clear()       // 첫 로드는 안내를 보내지 않지만, 테스트 대상 이벤트만 보도록 비운다

            block(blockRepository, viewModel, events)

            collectors.cancel()
            viewModel.viewModelScope.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private object OnlineNetworkMonitor : NetworkMonitor {
        override fun isOnline() = true
        override fun observeIsOnline() = flowOf(true)
    }

    private object UnusedReportRepository : CommunityReportRepository {
        override suspend fun createReport(
            reporterUid: String, targetType: String, targetId: String,
            parentPostId: String?, reason: ReportReason, detail: String?
        ): Result<Unit> = error("Unused")
    }

    private fun blocked(uid: String) = BlockedUser(uid = uid, nickname = "닉네임$uid", profileImg = "1", createdAt = 0L)

    private fun post(authorId: String) = Post.createOriginal(
        author = PostAuthor(id = authorId, nickname = "작성자", profileImg = "1"),
        title = "제목",
        content = "내용",
        tag = Tag(id = "tag1", name = "공부")
    ).copy(postId = POST_ID, commentCount = 3)

    private fun comment(id: String, uid: String, nickname: String) = Comment(
        commentId = id,
        user = CommentUser(uid, nickname, "1"),
        content = "댓글",
        activityId = "",
        createdAt = 0L,
        reportCount = 0,
        isHidden = false,
        isPending = false
    )

    private companion object {
        const val ME = "me"
        const val AUTHOR = "author"
        const val POST_ID = "post-1"
    }
}
