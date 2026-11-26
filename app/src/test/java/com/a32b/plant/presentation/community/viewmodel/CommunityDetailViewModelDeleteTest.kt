package com.a32b.plant.presentation.community.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostAuthor
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
import kotlinx.coroutines.CompletableDeferred
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
import org.junit.Test

/** 게시글 삭제 확인 연타 (#672) */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityDetailViewModelDeleteTest {

    @Test
    fun `삭제 응답을 기다리는 중 다시 눌러도 삭제 요청과 뒤로가기는 한 번만 간다`() = runDeleteTest { repository, notifier, viewModel, events ->
        val gate = CompletableDeferred<Unit>()
        repository.deletePostGate = gate

        viewModel.openDeleteDialog()
        viewModel.deletePost()
        viewModel.deletePost()   // 응답 대기 중 (게이트 미완료)
        runCurrent()
        gate.complete(Unit)
        runCurrent()

        assertEquals(listOf(POST_ID), repository.deletedPostIds)
        assertEquals(listOf(POST_ID), notifier.deleted)
        assertEquals(listOf<CommunityDetailEvent>(CommunityDetailEvent.NavigateBack), events)
        assertEquals(false, viewModel.showDeleteDialog.value)
    }

    @Test
    fun `삭제가 실패하면 다시 눌러 재시도할 수 있다`() = runDeleteTest { repository, _, viewModel, events ->
        repository.deletePostResult = Result.Failure(AppError.Network())

        viewModel.openDeleteDialog()
        viewModel.deletePost()
        runCurrent()
        repository.deletePostResult = Result.Success(Unit)
        viewModel.openDeleteDialog()
        viewModel.deletePost()
        runCurrent()

        assertEquals(listOf(POST_ID, POST_ID), repository.deletedPostIds)
        assertEquals(
            listOf(CommunityDetailEvent.ShowToast(AppError.Network().message), CommunityDetailEvent.NavigateBack),
            events
        )
    }

    private fun runDeleteTest(
        block: suspend TestScope.(
            FakeCommunityRepository, RecordingPostUpdateNotifier, CommunityDetailViewModel, List<CommunityDetailEvent>
        ) -> Unit
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
                postDetailResult = Result.Success(myPost()),
                commentsResult = Result.Success(emptyList())
            )
            val blockRepository = FakeBlockRepository()
            val notifier = RecordingPostUpdateNotifier()

            val viewModel = CommunityDetailViewModel(
                repository = communityRepository,
                addCommentUseCase = AddCommentUseCase(communityRepository, ensureCurrentUser),
                toggleLikeUseCase = ToggleLikeUseCase(communityRepository, ensureCurrentUser),
                toggleBookmarkUseCase = ToggleBookmarkUseCase(communityRepository, ensureCurrentUser),
                createCommunityReportUseCase = CreateCommunityReportUseCase(UnusedReportRepository, ensureCurrentUser),
                blockUserUseCase = BlockUserUseCase(blockRepository, ensureCurrentUser),
                observeBlockedUsersUseCase = ObserveBlockedUsersUseCase(blockRepository, userRepository),
                ensureCurrentUserUseCase = ensureCurrentUser,
                postUpdateNotifier = notifier,
                networkMonitor = OnlineNetworkMonitor,
                savedStateHandle = SavedStateHandle(mapOf("postId" to POST_ID))
            )
            val events = mutableListOf<CommunityDetailEvent>()
            val collector = launch(dispatcher) { viewModel.event.collect { events += it } }
            advanceUntilIdle()   // loadInitial
            events.clear()

            block(communityRepository, notifier, viewModel, events)

            collector.cancel()
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

    private fun myPost() = Post.createOriginal(
        author = PostAuthor(id = ME, nickname = "나", profileImg = "1"),
        title = "제목",
        content = "내용",
        tag = Tag(id = "tag1", name = "공부")
    ).copy(postId = POST_ID)

    private companion object {
        const val ME = "me"
        const val POST_ID = "post-1"
    }
}
