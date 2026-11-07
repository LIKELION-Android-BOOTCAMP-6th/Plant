package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.Feedback
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.result.Result

interface FeedbackRepository {
    fun createFeedbackId(): String

    suspend fun createFeedback(
        feedbackId: String,
        feedback: Feedback
    ): Result<FeedbackSubmissionResult>
}
