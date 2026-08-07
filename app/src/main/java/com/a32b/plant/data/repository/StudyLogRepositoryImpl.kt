package com.a32b.plant.data.repository

import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.StudyLogRepository
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
class StudyLogRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore
) : StudyLogRepository {

    override fun getStudyLogs(uid: String, potId: String): Flow<List<StudyLog>> = callbackFlow {
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
                    doc.toObject(StudyLog::class.java)?.copy(id = doc.id)
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    override suspend fun getPotLogs(uid: String, potId: String): List<StudyLog> {
        val snapshot = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .collection("logs")
            .orderBy("createAt", Query.Direction.DESCENDING)
            .get().await()
        return snapshot.documents.mapNotNull { it.toObject(StudyLog::class.java)?.copy(id = it.id) }
    }

    override suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): StudyLog? {
        val doc = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .collection("logs").document(logId)
            .get().await()
        return doc.toObject(StudyLog::class.java)?.copy(id = doc.id)
    }

    override fun createStudyLog(uid: String, potId: String, studyLog: StudyLog) {
        val docRef = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .collection("logs").document()
        docRef.set(studyLog.copy(id = docRef.id))
    }

    override suspend fun deleteStudyLog(uid: String, potId: String, logId: String, studyingTime: Long) {
        val userRef = db.collection("users").document(uid)
        val potRef = userRef.collection("pots").document(potId)
        val logRef = potRef.collection("logs").document(logId)

        val decreaseAmount = studyingTime * -1
        db.runBatch { batch ->
            batch.delete(logRef)
            batch.update(userRef, "totalStudyTime", FieldValue.increment(decreaseAmount))
            batch.update(potRef, "potTotalStudyingTime", FieldValue.increment(decreaseAmount))
        }.await()
    }
}