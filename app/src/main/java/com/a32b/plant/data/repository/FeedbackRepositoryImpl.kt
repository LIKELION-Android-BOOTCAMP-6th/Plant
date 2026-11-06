package com.a32b.plant.data.repository

import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.firestore.WriteResult
import com.a32b.plant.data.firestore.awaitServerWrite
import com.a32b.plant.data.mapper.toFirestoreMap
import com.a32b.plant.data.source.remote.feedback.FeedbackRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.repository.FeedbackRepository
import com.a32b.plant.domain.result.Result
import com.google.firebase.firestore.FirebaseFirestoreException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Singleton
class FeedbackRepositoryImpl @Inject constructor(
    private val remote: FeedbackRemoteDataSource
) : FeedbackRepository {
    override fun createFeedbackId(): String = remote.createFeedbackId()

    override suspend fun createFeedback(
        feedbackId: String,
        feedback: Feedback
    ): Result<FeedbackSubmissionResult> = safeRunCatching {
        // 쓰기를 시작하고(대기열 등록) 서버 반영을 제한 시간만큼만 기다린다.
        // 오프라인이면 응답이 오지 않으므로 시간 초과(Pending)는 실패가 아니라 "전송 대기"로 처리한다.
        // 쓰기는 대기열에 남아 연결되면 전송된다. 같은 ID로 다시 보내면 규칙(create만 허용)상
        // 수정으로 평가돼 거부되므로, 같은 화면에서 다시 보내지 않는다.
        remote.createFeedback(feedbackId, feedback.toFirestoreMap())
            .awaitServerWrite(SUBMISSION_TIMEOUT)
    }.fold(
        onSuccess = { write ->
            when (write) {
                WriteResult.Confirmed -> Result.Success(FeedbackSubmissionResult.Saved)
                WriteResult.Pending -> Result.Success(FeedbackSubmissionResult.Pending)
                is WriteResult.Rejected -> Result.Failure(handleError(write.error))
            }
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
        private val SUBMISSION_TIMEOUT = 10.seconds
        private const val GENERIC_ERROR_MESSAGE = "제출 처리 중 오류가 발생했습니다."
    }
}
