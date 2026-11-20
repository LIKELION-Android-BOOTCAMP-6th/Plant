package com.a32b.plant.presentation.community.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.model.User
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
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CommunityActivityViewModelTest {

    @Test
    fun 삭제_응답_전에_화면을_나가도_삭제가_끝나면_목록에_알린다() = runActivityTest { repository, notifier, viewModel, _ ->
        val gate = CompletableDeferred<Unit>()
        repository.deletePostGate = gate
        selectAndDelete(viewModel, "a1")
        assertEquals(listOf("post-1"), repository.deletedPostIds)
        assertTrue(notifier.deleted.isEmpty())

        viewModel.viewModelScope.cancel()   // 화면 이탈
        gate.complete(Unit)                  // 그 뒤에 서버 응답
        runCurrent()

        assertEquals(listOf("post-1"), notifier.deleted)
    }

    @Test
    fun 화면에_머문_채_삭제가_끝나면_목록에_알리고_선택을_해제하고_안내한다() = runActivityTest { _, notifier, viewModel, events ->
        selectAndDelete(viewModel, "a1")

        assertEquals(listOf("post-1"), notifier.deleted)
        assertFalse(viewModel.uiState.value.isLoading)
        assertFalse(viewModel.uiState.value.isSelectionMode)
        assertEquals(listOf<CommunityActivityEvent>(CommunityActivityEvent.ShowToast("삭제되었습니다.")), events)
    }

    // ---------- 도우미 ----------

    private fun TestScope.selectAndDelete(viewModel: CommunityActivityViewModel, activityId: String) {
        viewModel.toggleSelectionMode()
        viewModel.toggleItemSelection(activityId)
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
