package com.a32b.plant.data.datasource.community

import android.util.Log
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
): CommunityRemoteDataSource {
    override suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity) {
        val commentRef = db.collection("posts").document(postId)
            .collection("comments").document()
        val activityRef = db.collection("activities").document()

        val commentWithAct = comment.copy(activityId = activityRef.id, commentId = commentRef.id)
        val activityWithCo = activity.copy(targetId = postId, commentId = commentRef.id)
        Log.d("댓글", commentRef.id)

        db.runBatch { batch ->
            batch.set(commentRef, commentWithAct.toDto())
            batch.set(activityRef, activityWithCo.toDto())
        }.await()

        db.collection("posts").document(postId)
            .update("commentCount", FieldValue.increment(1))
    }


}