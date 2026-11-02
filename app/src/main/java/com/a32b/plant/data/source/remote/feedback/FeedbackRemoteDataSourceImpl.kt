package com.a32b.plant.data.source.remote.feedback

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : FeedbackRemoteDataSource {
    override fun createFeedbackId(): String =
        db.collection(FEEDBACKS_COLLECTION).document().id

    override suspend fun createFeedback(feedbackId: String, data: Map<String, Any>) {
        db.collection(FEEDBACKS_COLLECTION)
            .document(feedbackId)
            .set(data)
            .await()
    }

    companion object {
        private const val FEEDBACKS_COLLECTION = "feedbacks"
    }
}
