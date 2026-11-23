package com.a32b.plant.presentation.community.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.ActivityType
import com.a32b.plant.domain.usecase.community.DeleteActivitiesUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeCommunityRepository
import com.a32b.plant.fake.FakeUserRepository
import com.a32b.plant.fake.RecordingPostUpdateNotifier
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * '내 활동' 선택 삭제 중 화면을 나가도 목록에 삭제 신호가 가는지 확인한다 (#712).
 * 화면 이탈은 ViewModel이 정리될 때처럼 viewModelScope를 취소해 재현한다.
 * 선택 모드 해제(exitSelectionMode)가 모드를 끄기만 하는지도 확인한다 (#748).
 * 삭제 중 로딩 표시가 목록 도착·탭 로딩과 섞이지 않고 삭제가 끝날 때까지 유지되는지 확인한다 (#754).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityActivityViewModelTest {

    @Test
    fun `삭제 응답 전에 화면을 나가도 삭제가 끝나면 목록에 알린다`() = runActivityTest { repository, notifier, viewModel, events ->
        val gate = CompletableDeferred<Unit>()
        repository.deletePostGate = gate
        selectAndDelete(viewModel, "a1")
        assertEquals(listOf("post-1"), repository.deletedPostIds)
        assertTrue(notifier.deleted.isEmpty())

        viewModel.viewModelScope.cancel()   // 화면 이탈
        gate.complete(Unit)                  // 그 뒤에 서버 응답
        runCurrent()

        assertEquals(listOf("post-1"), notifier.deleted)
        // 블록 밖 결과 처리가 실행되더라도 Toast는 취소된 viewModelScope의 launch라 나가지 않는다
        assertTrue(events.isEmpty())
    }

    @Test
    fun `화면에 머문 채 삭제가 끝나면 목록에 알리고 선택을 해제하고 안내한다`() = runActivityTest { _, notifier, viewModel, events ->
        selectAndDelete(viewModel, "a1")

        assertEquals(listOf("post-1"), notifier.deleted)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
        assertEquals(listOf<CommunityActivityEvent>(CommunityActivityEvent.ShowToast("삭제되었습니다.")), events)
    }

    @Test
    fun `삭제 중 목록이 다시 와도 삭제가 끝날 때까지 로딩을 표시한다`() = runActivityTest { repository, _, viewModel, events ->
        val gate = CompletableDeferred<Unit>()
        repository.deletePostGate = gate
        repository.activities.value = listOf(activity("a1", "post-1"), activity("a2", "post-2"))
        runCurrent()
        selectAndDelete(viewModel, "a1", "a2")
        assertTrue(viewModel.uiState.value.isLoadingShown)

        // 한 항목의 activity가 먼저 지워져 목록이 다시 온 상황 (다른 항목은 아직 응답 대기)
        repository.activities.value = listOf(activity("a2", "post-2"))
        runCurrent()

        assertEquals(listOf("a2"), viewModel.uiState.value.activities.map { it.id })
        assertTrue(viewModel.uiState.value.isLoadingShown)
        assertTrue(events.isEmpty())

        gate.complete(Unit)
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoadingShown)
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertEquals(listOf<CommunityActivityEvent>(CommunityActivityEvent.ShowToast("삭제되었습니다.")), events)
    }

    @Test
    fun `삭제가 실패하면 로딩을 해제하고 선택을 유지한 채 안내한다`() = runActivityTest { repository, _, viewModel, events ->
        repository.deletePostResult = Result.Failure(AppError.Unknown("게시글 삭제에 실패했습니다."))
        selectAndDelete(viewModel, "a1")

        assertFalse(viewModel.uiState.value.isLoadingShown)
        assertTrue(viewModel.uiState.value.isSelectionMode)
        assertEquals(setOf("a1"), viewModel.uiState.value.selectedIds)
        assertEquals(listOf<CommunityActivityEvent>(CommunityActivityEvent.ShowToast("1개 중 1개 삭제에 실패했습니다.")), events)
    }

    @Test
    fun `삭제 중 다시 삭제를 요청하면 새로 시작하지 않는다`() = runActivityTest { repository, _, viewModel, events ->
        val gate = CompletableDeferred<Unit>()
        repository.deletePostGate = gate
        selectAndDelete(viewModel, "a1")

        viewModel.deleteSelected()
        runCurrent()
        assertEquals(listOf("post-1"), repository.deletedPostIds)

        gate.complete(Unit)
        runCurrent()

        assertFalse(viewModel.uiState.value.isLoadingShown)
        assertEquals(listOf<CommunityActivityEvent>(CommunityActivityEvent.ShowToast("삭제되었습니다.")), events)
    }

    @Test
    fun `삭제 중 탭을 바꾸면 삭제가 끝나도 새 탭 목록이 올 때까지 로딩을 표시한다`() = runActivityTest { repository, _, viewModel, _ ->
        val gate = CompletableDeferred<Unit>()
        repository.deletePostGate = gate
        repository.pendingActivityTypes = setOf(ActivityType.COMMENT)
        selectAndDelete(viewModel, "a1")

        viewModel.onSelectedChange(ActivityType.COMMENT)
        runCurrent()
        gate.complete(Unit)
        runCurrent()

        assertFalse(viewModel.uiState.value.isDeleting)
        assertTrue(viewModel.uiState.value.isLoading)
        assertTrue(viewModel.uiState.value.isLoadingShown)
    }

    @Test
    fun `삭제 없이 탭을 불러오면 목록이 올 때 로딩을 해제한다`() = runActivityTest { repository, _, viewModel, _ ->
        repository.pendingActivityTypes = setOf(ActivityType.COMMENT)
        viewModel.onSelectedChange(ActivityType.COMMENT)
        runCurrent()
        assertTrue(viewModel.uiState.value.isLoadingShown)

        repository.pendingActivityTypes = emptySet()
        viewModel.onSelectedChange(ActivityType.POST)
        runCurrent()

        assertEquals(listOf("a1"), viewModel.uiState.value.activities.map { it.id })
        assertFalse(viewModel.uiState.value.isLoadingShown)
    }

    @Test
    fun `선택 모드에서 해제하면 모드가 꺼지고 선택이 비워진다`() = runActivityTest { _, _, viewModel, _ ->
        viewModel.toggleSelectionMode()
        viewModel.toggleItemSelection("a1")

        viewModel.exitSelectionMode()

        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `선택 모드가 꺼진 상태에서 해제해도 다시 켜지지 않는다`() = runActivityTest { _, _, viewModel, _ ->
        viewModel.exitSelectionMode()

        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertTrue(viewModel.uiState.value.selectedIds.isEmpty())
    }

    // ---------- 도우미 ----------

    private fun TestScope.selectAndDelete(viewModel: CommunityActivityViewModel, vararg activityIds: String) {
        viewModel.toggleSelectionMode()
        activityIds.forEach { viewModel.toggleItemSelection(it) }
        viewModel.deleteSelected()
        runCurrent()
    }

    private fun runActivityTest(
        block: suspend TestScope.(
            FakeCommunityRepository,
            RecordingPostUpdateNotifier,
            CommunityActivityViewModel,
            List<CommunityActivityEvent>
        ) -> Unit
    ) = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val repository = FakeCommunityRepository()
            repository.activities.value = listOf(activity("a1", "post-1"))
            val notifier = RecordingPostUpdateNotifier()
            val viewModel = CommunityActivityViewModel(
                repository = repository,
                deleteActivitiesUseCase = DeleteActivitiesUseCase(
                    repository = repository,
                    ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
                        FakeUserRepository(User.create().copy(uid = "uid-1")),
                        object : SessionExpiredNotifier {
                            override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
                        }
                    ),
                    postUpdateNotifier = notifier
                )
            )
            val events = mutableListOf<CommunityActivityEvent>()
            val eventsJob = launch(dispatcher) { viewModel.event.collect { events += it } }
            block(repository, notifier, viewModel, events)
            eventsJob.cancel()
            viewModel.viewModelScope.cancel()   // 활동 목록 구독 정리
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun activity(id: String, targetId: String) = CommunityActivity(
        id = id,
        uid = "uid-1",
        type = ActivityType.POST,
        title = "제목",
        targetId = targetId,
        comment = null,
        commentId = null,
        createAt = null
    )
}
