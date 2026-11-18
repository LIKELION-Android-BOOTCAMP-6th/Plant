package com.a32b.plant.domain.usecase.feedback

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.FeedbackType
import com.a32b.plant.domain.type.ImprovementCategory
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeFeedbackRepository
import com.a32b.plant.fake.FakeUserRepository
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
}
