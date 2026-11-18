package com.a32b.plant.fake

import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.repository.FeedbackRepository
import com.a32b.plant.domain.result.Result

/**
 * 테스트 공용 [FeedbackRepository].
 * createFeedback은 [results]에 넣어 둔 결과를 순서대로 돌려주고, 비어 있으면 Saved를 돌려준다.
 */
class FakeFeedbackRepository : FeedbackRepository {
    var createFeedbackIdCalls = 0
    var createCalls = 0

    /** createFeedback 안에서 결과를 돌려주기 전에 실행된다 (요청 진행 중 상태를 만드는 게이트 등) */
    var onCreate: suspend () -> Unit = {}
    val results = mutableListOf<Result<FeedbackSubmissionResult>>()
    val submittedIds = mutableListOf<String>()

    /** 마지막으로 받은 feedbackId */
    var feedbackId: String? = null

    /** 마지막으로 받은 feedback */
    var feedback: Feedback? = null

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
        this.feedbackId = feedbackId
        this.feedback = feedback
        onCreate()
        return if (results.isEmpty()) {
            Result.Success(FeedbackSubmissionResult.Saved)
        } else {
            results.removeAt(0)
        }
    }
}
