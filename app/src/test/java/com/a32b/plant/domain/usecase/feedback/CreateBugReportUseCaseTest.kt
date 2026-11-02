package com.a32b.plant.domain.usecase.feedback

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.model.UserSnapshot
import com.a32b.plant.domain.repository.FeedbackRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.session.SessionExpiredNotifier
import com.a32b.plant.domain.session.SessionExpiredReason
import com.a32b.plant.domain.type.BugCategory
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
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
            deviceModel = "SM-G965N",
            androidVersion = "28",
            appVersion = "1.0"
        )

        assertTrue(result is Result.Success)
        assertEquals("feedback-1", repository.feedbackId)
        val feedback = repository.feedback!!
        assertEquals("bug", feedback.type.firestoreValue)
        assertEquals("uid-1", feedback.reporterId)
        assertEquals(BugCategory.FEATURE_ERROR, feedback.category)
        assertEquals("저장 버튼을 누르면 앱이 종료됩니다.", feedback.description)
        assertEquals("SM-G965N", feedback.deviceModel)
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

    private class FakeFeedbackRepository : FeedbackRepository {
        var feedbackId: String? = null
        var feedback: Feedback? = null
        var createCalls = 0

        override fun createFeedbackId(): String = "generated-id"

        override suspend fun createBugReport(
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
        override suspend fun registerNickname(nickname: String): Result<Unit> = error("Unused")
        override suspend fun deleteNickname(nickname: String): Result<Unit> = error("Unused")
        override suspend fun checkAttendance(uid: String): Result<AttendanceDecision.Success> = error("Unused")
        override suspend fun updateSessionToken(uid: String, token: String?): Result<Unit> = error("Unused")
        override suspend fun deleteUserData(uid: String): Result<Unit> = error("Unused")
    }
}