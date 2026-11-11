package com.a32b.plant.domain.usecase.feedback

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
import com.a32b.plant.domain.type.FeedbackType
import com.a32b.plant.domain.type.ImprovementCategory
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CreateImprovementSuggestionUseCaseTest {
    @Test
    fun createsImprovementFeedbackForCurrentUserWithoutBugFields() = runBlocking {
        val repository = FakeFeedbackRepository()
        val useCase = createUseCase(
            user = User.create().copy(uid = "uid-1"),
            repository = repository
        )

        val result = useCase(
            feedbackId = "feedback-1",
            category = ImprovementCategory.NEW_FEATURE,
            description = "학습 기록을 주간 단위로 볼 수 있으면 좋겠습니다.",
            appVersion = "1.0"
        )

        assertEquals(Result.Success(FeedbackSubmissionResult.Saved), result)
        assertEquals(1, repository.createCalls)
        assertEquals("feedback-1", repository.feedbackId)
        val feedback = repository.feedback!!
        assertEquals(FeedbackType.IMPROVEMENT, feedback.type)
        assertEquals("uid-1", feedback.reporterId)
        assertEquals(ImprovementCategory.NEW_FEATURE, feedback.category)
        assertEquals("학습 기록을 주간 단위로 볼 수 있으면 좋겠습니다.", feedback.description)
        assertEquals("1.0", feedback.appVersion)
        assertNull(feedback.reproductionSteps)
        assertNull(feedback.deviceModel)
        assertNull(feedback.androidVersion)
    }

    @Test
    fun currentUserLookupFailureIsReturnedWithoutCallingRepository() = runBlocking {
        val repository = FakeFeedbackRepository()
        val useCase = createUseCase(user = null, repository = repository)

        val result = useCase(
            feedbackId = "feedback-1",
            category = ImprovementCategory.OTHER,
            description = "제안 내용",
            appVersion = "1.0"
        )

        assertEquals(Result.Failure(AppError.UnknownUser()), result)
        assertEquals(0, repository.createCalls)
        assertNull(repository.feedback)
    }

    private fun createUseCase(
        user: User?,
        repository: FakeFeedbackRepository
    ) = CreateImprovementSuggestionUseCase(
        feedbackRepository = repository,
        ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
            FakeUserRepository(user),
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
            }
        )
    )

    private class FakeFeedbackRepository : FeedbackRepository {
        var feedbackId: String? = null
        var feedback: Feedback? = null
        var createCalls = 0

        override fun createFeedbackId(): String = "generated-id"

        override suspend fun createFeedback(
            feedbackId: String,
            feedback: Feedback
        ): Result<FeedbackSubmissionResult> {
            createCalls++
            this.feedbackId = feedbackId
            this.feedback = feedback
            return Result.Success(FeedbackSubmissionResult.Saved)
        }
    }

    private class FakeUserRepository(user: User?) : UserRepository {
        override val currentUser = MutableStateFlow(user)
        override fun observeDarkMode(): Flow<Boolean> = emptyFlow()
        override fun startUserSession(user: User): Unit = error("Unused")
        override fun endUserSession(): Unit = error("Unused")
        override fun takeExpiredSessionReason(): SessionExpiredReason? = error("Unused")
        override suspend fun updateLastSelectedPot(uid: String, potId: String): Unit = error("Unused")
        override suspend fun getUser(uid: String): Result<User?> = error("Unused")
        override suspend fun getUserPreferringServer(uid: String): Result<UserSnapshot> = error("Unused")
        override suspend fun createUser(uid: String): Result<User> = error("Unused")
        override suspend fun completeFirstLogin(uid: String, nickname: String): Result<Unit> = error("Unused")
        override suspend fun updateDarkMode(isDarkMode: Boolean): Result<Unit> = error("Unused")
        override suspend fun updateProfile(user: User): Result<Unit> = error("Unused")
        override suspend fun checkAttendance(): Result<AttendanceDecision.Success> = error("Unused")
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = error("Unused")
    }
}
