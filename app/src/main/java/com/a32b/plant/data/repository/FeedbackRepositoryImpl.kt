package com.a32b.plant.data.repository

import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.mapper.toFirestoreMap
import com.a32b.plant.data.source.remote.feedback.FeedbackRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.repository.FeedbackRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val remote: FeedbackRemoteDataSource
) : FeedbackRepository {
    override fun createFeedbackId(): String = remote.createFeedbackId()

    override suspend fun createFeedback(
        feedbackId: String,
        feedback: Feedback
    ): Result<FeedbackSubmissionResult> = safeRunCatching {
        // 오프라인 쓰기는 연결될 때까지 완료되지 않아 대기 시간을 10초로 제한한다.
        // 시간 초과는 실패가 아니라 전송 대기로 처리한다.
        withTimeoutOrNull(SUBMISSION_TIMEOUT_MILLIS) {
            remote.createFeedback(feedbackId, feedback.toFirestoreMap())
            true
        }
    }.fold(
        onSuccess = { completed ->
            Result.Success(
                if (completed == true) FeedbackSubmissionResult.Saved
                else FeedbackSubmissionResult.Pending
            )
        },
        onFailure = { error -> Result.Failure(handleError(error)) }
    )

    private fun handleError(error: Throwable): AppError = when (error) {
        is AppError -> error
        is FirebaseFirestoreException -> when (error.code) {
            FirebaseFirestoreException.Code.UNAVAILABLE,
            FirebaseFirestoreException.Code.DEADLINE_EXCEEDED -> AppError.Network()
            FirebaseFirestoreException.Code.PERMISSION_DENIED -> AppError.Permission()
            else -> AppError.Custom(GENERIC_ERROR_MESSAGE)
        }
        else -> AppError.Custom(GENERIC_ERROR_MESSAGE)
    }

    companion object {
        private const val SUBMISSION_TIMEOUT_MILLIS = 10_000L
        private const val GENERIC_ERROR_MESSAGE = "제출 처리 중 오류가 발생했습니다."
    }
}
