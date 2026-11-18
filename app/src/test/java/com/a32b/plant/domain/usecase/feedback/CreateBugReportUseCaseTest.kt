package com.a32b.plant.domain.usecase.feedback

import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.BugCategory
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.fake.FakeFeedbackRepository
import com.a32b.plant.fake.FakeUserRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CreateBugReportUseCaseTest {
    @Test
    fun createsBugFeedbackWithUserAndDeviceFieldsAndOmitsBlankSteps() = runBlocking {
        val users = FakeUserRepository(User.create().copy(uid = "uid-1"))
        val repository = FakeFeedbackRepository()
        val useCase = createUseCase(users, repository)

        val result = useCase(
            feedbackId = "feedback-1",
            category = BugCategory.FEATURE_ERROR,
            description = "저장 버튼을 누르면 앱이 종료됩니다.",
            reproductionSteps = "   ",
            deviceModel = "test-model",
            androidVersion = "28",
            appVersion = "1.0"
        )

        assertTrue(result is Result.Success)
        assertEquals("feedback-1", repository.feedbackId)
        val feedback = repository.feedback!!
        assertEquals("bug", feedback.type.firestoreKey)
        assertEquals("uid-1", feedback.reporterId)
        assertEquals(BugCategory.FEATURE_ERROR, feedback.category)
        assertEquals("저장 버튼을 누르면 앱이 종료됩니다.", feedback.description)
        assertEquals("test-model", feedback.deviceModel)
        assertEquals("28", feedback.androidVersion)
        assertEquals("1.0", feedback.appVersion)
        assertEquals(null, feedback.reproductionSteps)
        assertEquals(1, repository.createCalls)
    }

    @Test
    fun failedCurrentUserLookupDoesNotCallFeedbackRepository() = runBlocking {
        val users = FakeUserRepository(null)
        var sessionExpiredCalls = 0
        val repository = FakeFeedbackRepository()
        val useCase = CreateBugReportUseCase(
            feedbackRepository = repository,
            ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
                users,
                object : SessionExpiredNotifier {
                    override fun notifySessionExpired(reason: SessionExpiredReason) {
                        sessionExpiredCalls++
                    }
                }
            )
        )

        val result = useCase(
            feedbackId = "feedback-1",
            category = BugCategory.UI_BUG,
            description = "문제",
            reproductionSteps = null,
            deviceModel = "model",
            androidVersion = "28",
            appVersion = "1.0"
        )

        assertTrue(result is Result.Failure)
        assertEquals(1, sessionExpiredCalls)
        assertEquals(0, repository.createCalls)
    }

    private fun createUseCase(
        users: FakeUserRepository,
        repository: FakeFeedbackRepository
    ) = CreateBugReportUseCase(
        feedbackRepository = repository,
        ensureCurrentUserUseCase = EnsureCurrentUserUseCase(
            users,
            object : SessionExpiredNotifier {
                override fun notifySessionExpired(reason: SessionExpiredReason) = Unit
            }
        )
    )
}
