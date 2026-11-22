package com.a32b.plant.presentation.mypage.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.usecase.community.ObserveBlockedUsersUseCase
import com.a32b.plant.domain.usecase.community.UnblockUserUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeBlockRepository
import com.a32b.plant.fake.FakeUserRepository
import com.a32b.plant.presentation.community.CommunityConstants
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** 마이페이지 > 차단 목록 (#660) */
@OptIn(ExperimentalCoroutinesApi::class)
class BlockedUsersViewModelTest {

    @Test
    fun `차단 목록을 받으면 로딩을 끝내고 보여 준다`() = runBlockedTest { _, viewModel, _ ->
        assertFalse(viewModel.isLoading.value)
        assertEquals(listOf("a", "b"), viewModel.uiState.value.users.map { it.uid })
    }

    @Test
    fun `차단 목록을 받지 못해도 기다린 뒤 로딩을 끝낸다`() = runBlockedTest(neverEmits = true) { _, viewModel, _ ->
        assertTrue(viewModel.isLoading.value)
        advanceTimeBy(2_001L)
        assertFalse(viewModel.isLoading.value)
    }

    @Test
    fun `해제하면 목록에서 빠지고 안내한다`() = runBlockedTest { repository, viewModel, events ->
        viewModel.openUnblockDialog(viewModel.uiState.value.users.first())
        viewModel.confirmUnblock()
        runCurrent()

        assertEquals(listOf(ME to "a"), repository.unblockCalls)
        assertEquals(listOf("b"), viewModel.uiState.value.users.map { it.uid })
        assertNull(viewModel.uiState.value.unblockTarget)
        assertFalse(viewModel.uiState.value.isLoading)
        assertEquals(listOf<BlockedUsersEvent>(BlockedUsersEvent.ShowToast("차단을 해제했어요")), events)
    }

    @Test
    fun `오프라인이면 목록에서 바로 빠지고 대기 안내를 띄운다`() = runBlockedTest(
        unblockResult = Result.Success(SaveResult.Pending)
    ) { _, viewModel, events ->
        viewModel.openUnblockDialog(viewModel.uiState.value.users.first())
        viewModel.confirmUnblock()
        runCurrent()

        assertEquals(listOf("b"), viewModel.uiState.value.users.map { it.uid })
        assertEquals(listOf<BlockedUsersEvent>(BlockedUsersEvent.ShowToast(CommunityConstants.WRITE_PENDING_MESSAGE)), events)
    }

    @Test
    fun `해제가 실패하고 목록이 그대로면 다이얼로그를 유지하고 안내한다`() = runBlockedTest(
        unblockResult = Result.Failure(AppError.Custom("차단 해제에 실패했어요."))
    ) { _, viewModel, events ->
        viewModel.openUnblockDialog(viewModel.uiState.value.users.first())
        viewModel.confirmUnblock()
        runCurrent()

        assertEquals("a", viewModel.uiState.value.unblockTarget?.uid)
        assertEquals(listOf("a", "b"), viewModel.uiState.value.users.map { it.uid })
        assertEquals(listOf<BlockedUsersEvent>(BlockedUsersEvent.ShowToast("차단 해제에 실패했어요.")), events)
    }

    @Test
    fun `해제 처리 중에는 다른 사용자의 해제 창을 열지 않는다`() = runBlockedTest { repository, viewModel, _ ->
        val gate = CompletableDeferred<Unit>()
        repository.unblockGate = gate
        viewModel.openUnblockDialog(viewModel.uiState.value.users.first { it.uid == "a" })
        viewModel.confirmUnblock()
        runCurrent()
        // 기기 캐시에 먼저 반영돼 a가 목록에서 빠지고 확인 창이 닫혔지만, 해제 처리는 아직 진행 중
        assertTrue(viewModel.uiState.value.isLoading)
        assertNull(viewModel.uiState.value.unblockTarget)

        viewModel.openUnblockDialog(viewModel.uiState.value.users.first { it.uid == "b" })
        assertNull(viewModel.uiState.value.unblockTarget)

        gate.complete(Unit)
        runCurrent()
        assertFalse(viewModel.uiState.value.isLoading)
        viewModel.openUnblockDialog(viewModel.uiState.value.users.first { it.uid == "b" })
        assertEquals("b", viewModel.uiState.value.unblockTarget?.uid)
    }

    @Test
    fun `확인 중인 사용자가 다른 기기에서 먼저 해제되면 다이얼로그를 닫는다`() = runBlockedTest { repository, viewModel, _ ->
        viewModel.openUnblockDialog(viewModel.uiState.value.users.first())
        repository.blockedOf(ME).value = listOf(blocked("b"))
        runCurrent()

        assertNull(viewModel.uiState.value.unblockTarget)
    }

    // ---------- 도우미 ----------

    private fun runBlockedTest(
        neverEmits: Boolean = false,
        unblockResult: Result<SaveResult> = Result.Success(SaveResult.Saved),
        block: suspend TestScope.(FakeBlockRepository, BlockedUsersViewModel, List<BlockedUsersEvent>) -> Unit
    ) = runTest {
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        try {
            Dispatchers.setMain(dispatcher)
            val userRepository = FakeUserRepository(User.create().copy(uid = ME))
            val ensureCurrentUser = EnsureCurrentUserUseCase(
                userRepository,
                object : SessionExpiredNotifier {
                    override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
                }
            )
            val repository = FakeBlockRepository(unblockResult = unblockResult)
            repository.emitsBlockedUsers = !neverEmits
            repository.blockedOf(ME).value = listOf(blocked("a"), blocked("b"))
            val viewModel = BlockedUsersViewModel(
                observeBlockedUsersUseCase = ObserveBlockedUsersUseCase(repository, userRepository),
                unblockUserUseCase = UnblockUserUseCase(repository, ensureCurrentUser)
            )
            val events = mutableListOf<BlockedUsersEvent>()
            val eventsJob = launch(dispatcher) { viewModel.event.collect { events += it } }

            block(repository, viewModel, events)

            eventsJob.cancel()
            viewModel.viewModelScope.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun blocked(uid: String) = BlockedUser(uid = uid, nickname = "닉네임$uid", profileImg = "1", createdAt = 0L)

    private companion object {
        const val ME = "me"
    }
}
