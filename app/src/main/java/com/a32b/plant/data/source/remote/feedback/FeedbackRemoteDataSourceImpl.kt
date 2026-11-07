package com.a32b.plant.data.source.remote.feedback

import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : FeedbackRemoteDataSource {
    override fun createFeedbackId(): String =
        db.collection(FEEDBACKS_COLLECTION).document().id

    override fun createFeedback(feedbackId: String, data: Map<String, Any>): Task<Void> =
        db.collection(FEEDBACKS_COLLECTION)
            .document(feedbackId)
            .set(data)

    companion object {
        private const val FEEDBACKS_COLLECTION = "feedbacks"
    }
}
