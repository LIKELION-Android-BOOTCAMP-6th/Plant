package com.a32b.plant.presentation.feedback.viewmodel

import android.content.ContextWrapper
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.FeedbackRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.ImprovementCategory
import com.a32b.plant.domain.usecase.feedback.CreateImprovementSuggestionUseCase
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
class ImprovementSuggestionViewModelTest {
    @Test
    fun canSubmitRequiresCategoryNonBlankDescriptionAndNoLoading() {
        assertFalse(ImprovementSuggestionUiState().canSubmit)
        assertFalse(ImprovementSuggestionUiState(category = ImprovementCategory.UI_IMPROVEMENT).canSubmit)
        assertFalse(
            ImprovementSuggestionUiState(
                category = ImprovementCategory.UI_IMPROVEMENT,
                description = "   "
            ).canSubmit
        )
        assertFalse(
            ImprovementSuggestionUiState(
                category = ImprovementCategory.UI_IMPROVEMENT,
                description = "제안 내용",
                isLoading = true
            ).canSubmit
        )
        assertTrue(
            ImprovementSuggestionUiState(
                category = ImprovementCategory.UI_IMPROVEMENT,
                description = "제안 내용"
            ).canSubmit
        )
    }

    @Test
    fun repeatedSubmitWhileLoadingCallsRepositoryOnceAndNavigatesAfterSaved() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val requestStarted = CompletableDeferred<Unit>()
            val finishRequest = CompletableDeferred<Unit>()
            val repository = FakeFeedbackRepository().apply {
                onCreate = {
                    requestStarted.complete(Unit)
                    finishRequest.await()
                }
            }
            val viewModel = createViewModel(repository)
            val events = collectEvents(viewModel)
            viewModel.onCategoryChanged(ImprovementCategory.NEW_FEATURE)
            viewModel.onDescriptionChanged("주간 학습 요약을 추가해주세요.")

            viewModel.submit()
            runCurrent()
            assertTrue(viewModel.uiState.value.isLoading)
            viewModel.submit()
            runCurrent()

            assertEquals(1, repository.createCalls)
            finishRequest.complete(Unit)
            runCurrent()
            assertTrue(requestStarted.isCompleted)
            assertEquals(
                listOf(
                    ImprovementSuggestionEvent.ShowToast("개선사항 제안이 접수되었습니다."),
                    ImprovementSuggestionEvent.NavigateBack
                ),
                events
            )
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun immediateFailureKeepsInputAndSameIdAvailableForRetry() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeFeedbackRepository().apply {
                results += Result.Failure(AppError.Permission())
                results += Result.Success(FeedbackSubmissionResult.Saved)
            }
            val viewModel = createViewModel(repository)
            val events = collectEvents(viewModel)
            viewModel.onCategoryChanged(ImprovementCategory.CONVENIENCE)
            viewModel.onDescriptionChanged("알림 설정을 개선해주세요.")

            viewModel.submit()
            runCurrent()

            assertEquals(
                listOf(ImprovementSuggestionEvent.ShowToast(AppError.Permission().message)),
                events
            )
            assertEquals("알림 설정을 개선해주세요.", viewModel.uiState.value.description)
            assertEquals(ImprovementCategory.CONVENIENCE, viewModel.uiState.value.category)
            assertFalse(viewModel.uiState.value.isLoading)
            assertTrue(viewModel.uiState.value.canSubmit)

            viewModel.submit()
            runCurrent()

            assertEquals(2, repository.createCalls)
            assertEquals(listOf("feedback-1", "feedback-1"), repository.submittedIds)
            assertEquals(
                listOf(
                    ImprovementSuggestionEvent.ShowToast(AppError.Permission().message),
                    ImprovementSuggestionEvent.ShowToast("개선사항 제안이 접수되었습니다."),
                    ImprovementSuggestionEvent.NavigateBack
                ),
                events
            )
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun pendingShowsNoticeNavigatesBackAndDoesNotSubmitAgain() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        try {
            val repository = FakeFeedbackRepository().apply {
                results += Result.Success(FeedbackSubmissionResult.Pending)
            }
            val viewModel = createViewModel(repository)
            val events = collectEvents(viewModel)
            viewModel.onCategoryChanged(ImprovementCategory.OTHER)
            viewModel.onDescriptionChanged("검색 기능이 필요합니다.")

            viewModel.submit()
            runCurrent()
            viewModel.submit()
            runCurrent()

            assertEquals(1, repository.createCalls)
            assertEquals(
                listOf(
                    ImprovementSuggestionEvent.ShowToast(
                        NETWORK_SLOW_MESSAGE
                    ),
                    ImprovementSuggestionEvent.NavigateBack
                ),
                events
            )
            assertFalse(viewModel.uiState.value.isLoading)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createViewModel(repository: FakeFeedbackRepository): ImprovementSuggestionViewModel {
        val userRepository = FakeUserRepository(User.create().copy(uid = "uid-1"))
        val useCase = CreateImprovementSuggestionUseCase(
            feedbackRepository = repository,
            ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
                userRepository,
                object : SessionExpiredNotifier {
                    override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
                }
            )
        )
        return ImprovementSuggestionViewModel(
            createImprovementSuggestionUseCase = useCase,
            feedbackRepository = repository,
            context = ContextWrapper(null)
        )
    }

    private fun kotlinx.coroutines.test.TestScope.collectEvents(
        viewModel: ImprovementSuggestionViewModel
    ): MutableList<ImprovementSuggestionEvent> {
        val events = mutableListOf<ImprovementSuggestionEvent>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.events.toList(events)
        }
        return events
    }

    private class FakeFeedbackRepository : FeedbackRepository {
        var createCalls = 0
        var onCreate: suspend () -> Unit = {}
        val results = mutableListOf<Result<FeedbackSubmissionResult>>()
        val submittedIds = mutableListOf<String>()

        override fun createFeedbackId(): String = "feedback-1"

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
        override fun beginAccountDeletion(uid: String): Unit = error("Unused")
        override fun endAccountDeletion(): Unit = error("Unused")
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
        override suspend fun deleteUserData(uid: String): Result<Unit> = error("Unused")
    }
}
