package com.a32b.plant.data.source.remote.feedback

import com.a32b.plant.domain.model.Feedback

interface FeedbackRemoteDataSource {
    fun createFeedbackId(): String

    suspend fun createFeedback(
        feedbackId: String,
        feedback: Feedback
    )
}
