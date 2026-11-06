package com.a32b.plant.data.source.remote.feedback

import com.google.android.gms.tasks.Task

interface FeedbackRemoteDataSource {
    fun createFeedbackId(): String

    /** 쓰기를 시작하고, 서버 반영을 나타내는 Task를 돌려준다. */
    fun createFeedback(
        feedbackId: String,
        data: Map<String, Any>
    ): Task<Void>
}
