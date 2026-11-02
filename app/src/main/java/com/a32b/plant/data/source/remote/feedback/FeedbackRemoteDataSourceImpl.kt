package com.a32b.plant.data.source.remote.feedback

import com.a32b.plant.domain.model.Feedback
import com.google.firebase.firestore.FieldValue
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

    override suspend fun createFeedback(feedbackId: String, feedback: Feedback) {
        db.collection(FEEDBACKS_COLLECTION)
            .document(feedbackId)
            .set(feedback.toFirestoreMap())
            .await()
    }

    companion object {
        private const val FEEDBACKS_COLLECTION = "feedbacks"
    }
}

fun Feedback.toFirestoreMap(): Map<String, Any> = buildMap {
    put("type", type.firestoreValue)
    put("reporterId", reporterId)
    put("category", category.firestoreValue)
    put("description", description)
    put("appVersion", appVersion)
    put("createdAt", FieldValue.serverTimestamp())
    put("status", "pending")
    reproductionSteps?.let { put("reproductionSteps", it) }
    put("deviceModel", deviceModel)
    put("androidVersion", androidVersion)
}