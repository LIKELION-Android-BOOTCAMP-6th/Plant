package com.a32b.plant.data.repository

import com.a32b.plant.data.source.remote.feedback.FeedbackRemoteDataSource
import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.BugCategory
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FeedbackRepositoryImplTest {
    @Test
    fun completedWriteReturnsSaved() = runTest {
        val repository = FeedbackRepositoryImpl(FakeFeedbackRemoteDataSource())

        val result = repository.createFeedback("id-1", createFeedback())

        assertEquals(Result.Success(FeedbackSubmissionResult.Saved), result)
    }

    @Test
    fun timeoutReturnsPendingInsteadOfFailure() = runTest {
        val remote = FakeFeedbackRemoteDataSource().apply {
            onCreate = { awaitCancellation() }
        }
        val repository = FeedbackRepositoryImpl(remote)

        val result = repository.createFeedback("id-1", createFeedback())

        assertEquals(Result.Success(FeedbackSubmissionResult.Pending), result)
    }

    @Test
    fun otherErrorsUseFeedbackFailureMessage() = runTest {
        val remote = FakeFeedbackRemoteDataSource().apply {
            onCreate = { throw IllegalStateException("unexpected") }
        }
        val repository = FeedbackRepositoryImpl(remote)

        val result = repository.createFeedback("id-1", createFeedback())

        assertTrue(result is Result.Failure)
        assertEquals(
            "제보 처리 중 오류가 발생했습니다.",
            (result as Result.Failure).error.message
        )
    }

    private fun createFeedback() = Feedback.createBug(
        reporterId = "uid-1",
        category = BugCategory.OTHER,
        description = "문제",
        reproductionSteps = null,
        deviceModel = "model",
        androidVersion = "28",
        appVersion = "1.0"
    )

    private class FakeFeedbackRemoteDataSource : FeedbackRemoteDataSource {
        var onCreate: suspend () -> Unit = {}
        override fun createFeedbackId(): String = "id-1"
        override suspend fun createFeedback(feedbackId: String, feedback: Feedback) = onCreate()
    }
}
