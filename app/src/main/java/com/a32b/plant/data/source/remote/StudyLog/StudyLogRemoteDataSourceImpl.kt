package com.a32b.plant.data.datasource

import com.a32b.plant.data.firestore.getPreferringCache
import com.a32b.plant.data.model.StudyLogDto
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
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

    override suspend fun getPotLogsInPeriod(
        uid: String,
        potId: String,
        startInclusive: Timestamp,
        endExclusive: Timestamp
    ): GetPotLogsInPeriodResult {
        val snapshot = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .collection("logs")
            .whereGreaterThanOrEqualTo("startedAt", startInclusive)
            .whereLessThan("startedAt", endExclusive)
            .get()
            .await()
        return GetPotLogsInPeriodResult(
            logs = snapshot.documents.mapNotNull { it.toObject(StudyLogDto::class.java) },
            isFromCache = snapshot.metadata.isFromCache
        )
    }

    private fun userRef(uid: String) = db.collection("users").document(uid)

    private fun potRef(uid: String, potId: String) = userRef(uid).collection("pots").document(potId)

    private fun logRef(uid: String, potId: String, logId: String) =
        potRef(uid, potId).collection("logs").document(logId)

    override suspend fun getStudyLogPreferringCache(uid: String, potId: String, logId: String): StudyLogDto? =
        logRef(uid, potId, logId).getPreferringCache()
            .takeIf { it.exists() }
            ?.toObject(StudyLogDto::class.java)

    override suspend fun existsStudyLogOnServer(uid: String, potId: String, logId: String): Boolean =
        logRef(uid, potId, logId).get(Source.SERVER).await().exists()

    override fun deleteStudyLog(uid: String, potId: String, logId: String, studyingTime: Long): Task<Void> =
        db.runBatch { batch ->
            batch.delete(logRef(uid, potId, logId))
            // 규칙이 차감이 어느 기록 때문인지 확인하도록 기록 ID를 함께 적는다
            batch.update(
                potRef(uid, potId),
                mapOf(
                    "potTotalStudyingTime" to FieldValue.increment(-studyingTime),
                    "lastDeletedLogId" to logId
                )
            )
            batch.update(
                userRef(uid),
                mapOf(
                    "totalStudyTime" to FieldValue.increment(-studyingTime),
                    "lastDeletedPotId" to potId,
                    "lastDeletedLogId" to logId
                )
            )
        }
}