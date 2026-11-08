package com.a32b.plant.presentation.feedback.viewmodel

import android.content.ContextWrapper
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.repository.FeedbackRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.BugCategory
import com.a32b.plant.domain.usecase.feedback.CreateBugReportUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class BugReportViewModelTest {
    @Test
    fun canSubmitRequiresCategoryNonBlankDescriptionAndNoLoading() {
        assertFalse(BugReportUiState().canSubmit)
        assertFalse(BugReportUiState(category = BugCategory.UI_BUG).canSubmit)
        assertFalse(
            BugReportUiState(
                category = BugCategory.UI_BUG,
                description = "   "
            ).canSubmit
        )
        assertTrue(
            BugReportUiState(
                category = BugCategory.UI_BUG,
                description = "문제"
            ).canSubmit
        )
        assertFalse(
            BugReportUiState(
                category = BugCategory.UI_BUG,
                description = "문제",
                isLoading = true
            ).canSubmit
        )
    }

    @Test
    fun rapidSubmitWhileLoadingCallsRepositoryOnceAndSavedNavigatesBack() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val gate = CompletableDeferred<Unit>()
            val repository = FakeFeedbackRepository().apply {
                onCreate = { gate.await() }
            }
            val viewModel = createViewModel(repository)
            val events = collectEvents(viewModel)

            assertEquals(1, repository.createFeedbackIdCalls)
            viewModel.onCategoryChanged(BugCategory.UI_BUG)
            viewModel.onDescriptionChanged("문제")
            viewModel.submit()
            viewModel.submit()
            runCurrent()

            assertEquals(1, repository.createCalls)
            assertTrue(viewModel.uiState.value.isLoading)

            gate.complete(Unit)
            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                listOf(
                    BugReportEvent.ShowToast("버그 제보가 접수되었습니다."),
                    BugReportEvent.NavigateBack
                ),
                events
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun immediateFailureKeepsInputAndAllowsRetryWithSameId() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeFeedbackRepository().apply {
                results += Result.Failure(AppError.Permission())
                results += Result.Success(FeedbackSubmissionResult.Saved)
            }
            val viewModel = createViewModel(repository)
            val events = collectEvents(viewModel)
            viewModel.onCategoryChanged(BugCategory.FEATURE_ERROR)
            viewModel.onDescriptionChanged("버튼이 동작하지 않습니다.")

            viewModel.submit()
            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.canSubmit)
            assertEquals("버튼이 동작하지 않습니다.", viewModel.uiState.value.description)
            assertEquals(listOf(BugReportEvent.ShowToast(AppError.Permission().message)), events)

            viewModel.submit()
            runCurrent()

            assertEquals(listOf("feedback-1", "feedback-1"), repository.submittedIds)
            assertEquals(1, repository.createFeedbackIdCalls)
            assertEquals(2, repository.createCalls)
            assertEquals(
                listOf(
                    BugReportEvent.ShowToast("버그 제보가 접수되었습니다."),
                    BugReportEvent.NavigateBack
                ),
                events.takeLast(2)
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun pendingShowsNonSuccessToastClosesAndCannotResubmit() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeFeedbackRepository().apply {
                results += Result.Success(FeedbackSubmissionResult.Pending)
            }
            val viewModel = createViewModel(repository)
            val events = collectEvents(viewModel)
            viewModel.onCategoryChanged(BugCategory.OTHER)
            viewModel.onDescriptionChanged("화면이 멈춥니다.")

            viewModel.submit()
            runCurrent()

            assertFalse(viewModel.uiState.value.isLoading)
            assertEquals(
                listOf(
                    BugReportEvent.ShowToast(NETWORK_SLOW_MESSAGE),
                    BugReportEvent.NavigateBack
                ),
                events
            )

            viewModel.submit()
            runCurrent()
            assertEquals(1, repository.createCalls)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(repository: FakeFeedbackRepository): BugReportViewModel {
        val userRepository = FakeUserRepository(User.create().copy(uid = "uid-1"))
        val useCase = CreateBugReportUseCase(
            feedbackRepository = repository,
            ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
                userRepository,
                object : SessionExpiredNotifier {
                    override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
                }
            )
        )
        return BugReportViewModel(
            createBugReportUseCase = useCase,
            feedbackRepository = repository,
            context = ContextWrapper(null)
        )
    }

    private fun kotlinx.coroutines.test.TestScope.collectEvents(
        viewModel: BugReportViewModel
    ): MutableList<BugReportEvent> {
        val events = mutableListOf<BugReportEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    private class FakeFeedbackRepository : FeedbackRepository {
        var createFeedbackIdCalls = 0
        var createCalls = 0
        var onCreate: suspend () -> Unit = {}
        val results = mutableListOf<Result<FeedbackSubmissionResult>>()
        val submittedIds = mutableListOf<String>()

        override fun createFeedbackId(): String {
            createFeedbackIdCalls++
            // 호출마다 다른 ID를 돌려줘야, 재시도 때 ID를 새로 만드는 회귀를 테스트가 잡는다
            return "feedback-$createFeedbackIdCalls"
        }

        override suspend fun createFeedback(
            feedbackId: String,
            feedback: Feedback
        ): Result<FeedbackSubmissionResult> {
            createCalls++
            submittedIds += feedbackId
            onCreate()
            return if (results.isEmpty()) {
                Result.Success(FeedbackSubmissionResult.Saved)
            } else {
                results.removeAt(0)
            }
        }
    }

    private class FakeUserRepository(user: User?) : UserRepository {
        override val currentUser = MutableStateFlow(user)
        override fun observeDarkMode(): Flow<Boolean> = emptyFlow()
        override fun startUserSession(user: User): Unit = error("Unused")
        override fun endUserSession(): Unit = error("Unused")
        override suspend fun updateLastSelectedPot(uid: String, potId: String): Unit = error("Unused")
        override suspend fun getUser(uid: String): Result<User?> = error("Unused")
        override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> = error("Unused")
        override suspend fun createUser(uid: String): Result<User> = error("Unused")
        override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> = error("Unused")
        override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> = error("Unused")
        override suspend fun updateProfile(user: User): Result<Unit> = error("Unused")
        override suspend fun isNicknameTaken(nickname: String): Result<Boolean> = error("Unused")
        override suspend fun registerNickname(uid: String, nickname: String): Result<Unit> = error("Unused")
        override suspend fun deleteNickname(nickname: String): Result<Unit> = error("Unused")
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = error("Unused")
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = error("Unused")
    }
}
