package com.a32b.plant.data.source.remote.community

import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityReportRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : CommunityReportRemoteDataSource {

    override suspend fun createReport(
        reporterUid: String,
        targetType: String,
        targetId: String,
        parentPostId: String?,
        reason: String,
        detail: String?
    ): Boolean {
        val duplicate = db.collection("reports")
            .whereEqualTo("reporterId", reporterUid)
            .whereEqualTo("targetId", targetId)
            .limit(1)
            .get().await()

        if (!duplicate.isEmpty) return false

        val reportData = buildMap<String, Any?> {
            put("targetType", targetType)
            put("targetId", targetId)
            put("parentPostId", parentPostId)
            put("reporterId", reporterUid)
            put("reason", reason)
            put("detail", detail)
            put("createdAt", FieldValue.serverTimestamp())
            put("status", "pending")
        }
        db.collection("reports").add(reportData).await()

        val targetRef = when (targetType) {
            "post" -> db.collection("posts").document(targetId)
            "comment" -> {
                val postId = parentPostId ?: return true
                db.collection("posts").document(postId)
                    .collection("comments").document(targetId)
            }
            else -> return true
        }
        targetRef.update("reportCount", FieldValue.increment(1)).await()

        return true
    }
}
