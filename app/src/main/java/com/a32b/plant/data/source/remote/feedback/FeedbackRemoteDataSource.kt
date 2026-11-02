package com.a32b.plant.data.source.remote.feedback

interface FeedbackRemoteDataSource {
    fun createFeedbackId(): String

    suspend fun createFeedback(
        feedbackId: String,
        data: Map<String, Any>
    )
}
