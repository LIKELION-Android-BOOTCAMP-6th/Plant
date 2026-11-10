package com.a32b.plant.data.source.remote.studying

import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.data.model.StudyLogWithPotDto
import com.a32b.plant.data.model.StudySaveStateDto
import com.a32b.plant.data.model.StudyingUserDto
import com.a32b.plant.domain.error.AppError
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.async
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyingRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
): StudyingRemoteDataSource {
    private val uid: String
        get() = auth.currentUser?.uid ?: throw AppError.UnknownUser()
    override fun observeStudyingUser(tag: String): Flow<List<StudyingUserDto>> = callbackFlow {
        val listener = db.collection("studying")
            .whereEqualTo("tag", tag)
            .orderBy("studyingTime", Query.Direction.DESCENDING)
            .limit(3)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null){
                    close(exception)
                    return@addSnapshotListener
                }
                val users = snapshots?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(StudyingUserDto::class.java) }.getOrNull()
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove()}
    }

    override suspend fun saveStudyingUser(user: StudyingUserDto){
        // 규칙(validStudyingUser)이 허용하는 필드만 보내고, updatedAt은 서버 시각이어야 한다
        db.collection("studying")
            .document(user.uid)
            .set(
                mapOf(
                    "nickname" to user.nickname,
                    "profileImg" to user.profileImg,
                    "tag" to user.tag,
                    "studyingTime" to user.studyingTime,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            .await()
    }

    private fun userRef(uid: String) = db.collection("users").document(uid)

    private fun potRef(uid: String, potId: String) = userRef(uid).collection("pots").document(potId)

    private fun logRef(uid: String, potId: String, logId: String) =
        potRef(uid, potId).collection("logs").document(logId)

    override fun saveStudySession(uid: String, potId: String, sessionId: String, log: StudyLogDto): Task<Void> {
        val studyingTime = log.studyingTime
        // 규칙(userStudySaved)이 startedAt + 학습 시간과 같은지 비교하므로 startedAt과 같은 ms 단위 값으로 만든다
        val endedAt = Timestamp(Date(log.startedAt.toDate().time + studyingTime))
        // 규칙이 허용하는 필드만 보낸다 (DTO를 그대로 쓰면 reward: null 같은 필드가 함께 저장되어 거부됨)
        val logData = mapOf(
            "uid" to uid,
            "title" to log.title,
            "contents" to log.contents,
            "completedContents" to log.completedContents,
            "studyingTime" to studyingTime,
            "createAt" to log.createAt,
            "startedAt" to log.startedAt,
            "rewardStatus" to "pending",
            "rewardSeen" to false
        )
        return db.runBatch { batch ->
            batch.set(logRef(uid, potId, sessionId), logData)
            batch.update(
                potRef(uid, potId),
                mapOf(
                    "potTotalStudyingTime" to FieldValue.increment(studyingTime),
                    "lastStudiedAt" to FieldValue.serverTimestamp(),
                    "lastStudySessionId" to sessionId
                )
            )
            batch.update(
                userRef(uid),
                mapOf(
                    "totalStudyTime" to FieldValue.increment(studyingTime),
                    "lastStudyEndedAt" to endedAt,
                    "lastStudyPotId" to potId,
                    "lastStudySessionId" to sessionId
                )
            )
        }
    }

    override suspend fun getStudySaveState(uid: String, potId: String, sessionId: String): StudySaveStateDto = coroutineScope {
        val log = async { logRef(uid, potId, sessionId).get(Source.SERVER).await() }
        val pot = async { potRef(uid, potId).get(Source.SERVER).await() }
        val user = async { userRef(uid).get(Source.SERVER).await() }
        val potSnapshot = pot.await()
        StudySaveStateDto(
            logExists = log.await().exists(),
            potExists = potSnapshot.exists(),
            potCompleted = potSnapshot.getBoolean("isCompleted") == true,
            lastStudyEndedAt = user.await().getTimestamp("lastStudyEndedAt")
        )
    }

    override suspend fun existsStudyLogOnServer(uid: String, potId: String, sessionId: String): Boolean =
        logRef(uid, potId, sessionId).get(Source.SERVER).await().exists()

    override fun observeStudyLog(uid: String, potId: String, sessionId: String): Flow<StudyLogDto?> = callbackFlow {
        val listener = logRef(uid, potId, sessionId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            trySend(snapshot?.takeIf { it.exists() }?.toObject(StudyLogDto::class.java))
        }
        awaitClose { listener.remove() }
    }

    override fun observeUnseenRewardLogs(uid: String): Flow<List<StudyLogWithPotDto>> = callbackFlow {
        // 규칙(/{path=**}/logs)은 uid로 거른 조회만 허용하므로 uid 조건을 빼면 거부된다
        // collectionGroup 복합 색인 필요 (firestore.indexes.json)
        val listener = db.collectionGroup("logs")
            .whereEqualTo("uid", uid)
            .whereEqualTo("rewardStatus", "granted")
            .whereEqualTo("rewardSeen", false)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                val logs = snapshot?.documents?.mapNotNull { doc ->
                    // 경로: users/{uid}/pots/{potId}/logs/{logId}
                    val potId = doc.reference.parent.parent?.id ?: return@mapNotNull null
                    val log = runCatching { doc.toObject(StudyLogDto::class.java) }.getOrNull()
                        ?: return@mapNotNull null
                    StudyLogWithPotDto(potId, log)
                } ?: emptyList()
                trySend(logs)
            }
        awaitClose { listener.remove() }
    }

    override fun markRewardsSeen(uid: String, logs: List<Pair<String, String>>): Task<Void> =
        db.runBatch { batch ->
            logs.forEach { (potId, logId) ->
                batch.update(logRef(uid, potId, logId), "rewardSeen", true)
            }
        }

    override suspend fun deleteStudyingUserInfo(){
        db.collection("studying")
            .document(uid)
            .delete()
            .await()
    }
}
