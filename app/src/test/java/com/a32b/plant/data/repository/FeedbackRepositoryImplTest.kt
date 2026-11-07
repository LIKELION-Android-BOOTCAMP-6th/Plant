package com.a32b.plant.data.repository

import com.a32b.plant.data.source.remote.feedback.FeedbackRemoteDataSource
import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.BugCategory
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.TaskCompletionSource
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeedbackRepositoryImplTest {
    @Test
    fun serverAckReturnsSaved() = runTest {
        val remote = FakeFeedbackRemoteDataSource()
        remote.write.setResult(null)
        val repository = FeedbackRepositoryImpl(remote)

        val result = repository.createFeedback("id-1", createFeedback())

        assertEquals(Result.Success(FeedbackSubmissionResult.Saved), result)
    }

    // 오프라인: 쓰기는 대기열에 남아 응답이 오지 않는다 → 실패가 아니라 전송 대기(Pending)
    @Test
    fun noResponseWithinTimeoutReturnsPendingInsteadOfFailure() = runTest {
        val remote = FakeFeedbackRemoteDataSource()
        val repository = FeedbackRepositoryImpl(remote)

        val result = repository.createFeedback("id-1", createFeedback())

        assertEquals(Result.Success(FeedbackSubmissionResult.Pending), result)
        // 제한 시간이 10초인지도 확인한다 (runTest의 가상 시간 기준)
        assertEquals(10_000L, currentTime)
    }

    @Test
    fun serverRejectionReturnsFailureWithFeedbackMessage() = runTest {
        val remote = FakeFeedbackRemoteDataSource()
        remote.write.setException(IllegalStateException("rejected"))
        val repository = FeedbackRepositoryImpl(remote)

        val result = repository.createFeedback("id-1", createFeedback())

        assertTrue(result is Result.Failure)
        assertEquals(
            "제출 처리 중 오류가 발생했습니다.",
            (result as Result.Failure).error.message
        )
    }

    // Task를 만들기 전에 동기적으로 던지는 예외도 실패로 처리해야 한다
    @Test
    fun synchronousExceptionReturnsFailureWithFeedbackMessage() = runTest {
        val remote = FakeFeedbackRemoteDataSource().apply {
            onCreate = { throw IllegalStateException("invalid data") }
        }
        val repository = FeedbackRepositoryImpl(remote)

        val result = repository.createFeedback("id-1", createFeedback())

        assertTrue(result is Result.Failure)
        assertEquals(
            "제출 처리 중 오류가 발생했습니다.",
            (result as Result.Failure).error.message
        )
    }

    @Test
    fun passesFeedbackIdAndMappedPayloadToDataSource() = runTest {
        val remote = FakeFeedbackRemoteDataSource()
        remote.write.setResult(null)
        val repository = FeedbackRepositoryImpl(remote)

        repository.createFeedback("id-1", createFeedback())

        assertEquals("id-1", remote.lastFeedbackId)
        assertEquals("bug", remote.lastData?.get("type"))
        assertEquals("other", remote.lastData?.get("category"))
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
        val write = TaskCompletionSource<Void>()
        var onCreate: () -> Unit = {}
        var lastFeedbackId: String? = null
        var lastData: Map<String, Any>? = null

        override fun createFeedbackId(): String = "id-1"

        override fun createFeedback(feedbackId: String, data: Map<String, Any>): Task<Void> {
            onCreate()
            lastFeedbackId = feedbackId
            lastData = data
            return write.task
        }
    }
}
