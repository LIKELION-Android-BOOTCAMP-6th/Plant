package com.a32b.plant.data.datasource

import com.a32b.plant.data.model.StudyLogDto
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyLogRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : StudyLogRemoteDataSource {

    override fun getStudyLogs(uid: String, potId: String): Flow<List<StudyLogDto>> = callbackFlow {
        val listener = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .collection("logs")
            .orderBy("createAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(StudyLogDto::class.java)
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPotLogs(uid: String, potId: String): List<StudyLogDto> {
        val snapshot = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .collection("logs")
            .orderBy("createAt", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { it.toObject(StudyLogDto::class.java) }
    }

    override suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): StudyLogDto? {
        val doc = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .collection("logs").document(logId)
            .get().await()
        return doc.toObject(StudyLogDto::class.java)
    }

    override suspend fun executeDeleteBatch(uid: String, potId: String, logId: String, decreaseAmount: Long) {
        val userRef = db.collection("users").document(uid)
        val potRef = userRef.collection("pots").document(potId)
        val logRef = potRef.collection("logs").document(logId)

        db.runBatch { batch ->
            batch.delete(logRef)
            batch.update(userRef, "totalStudyTime", FieldValue.increment(decreaseAmount))
            batch.update(potRef, "potTotalStudyingTime", FieldValue.increment(decreaseAmount))
        }.await()
    }
}