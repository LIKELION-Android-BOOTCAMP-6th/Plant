package com.a32b.plant.data.source.remote.pot

import android.util.Log
import com.a32b.plant.data.firestore.getPreferringCache
import com.a32b.plant.data.model.PotDto
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.type.ItemType
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PotRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : PotRemoteDataSource {

    //화분 리스트 -> 실시간으로 구독 (Flow)
    override fun getPots(uid: String): Flow<List<PotDto>> = callbackFlow {
        if (uid.isEmpty()) {
            Log.e("PotRemoteDataSource", "UID가 비어있어 Firestore 리스너를 시작하지 않습니다.")
            close() // 혹은 trySend(emptyList())
            return@callbackFlow
        }
        val collectionRef = db.collection("users").document(uid).collection("pots")

        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }

            // Firebase -> PotDto 리스트 변환. 문서 ID는 doc.id로 직접 채운다
            // (필드 자체가 없거나 값이 어긋난 문서가 있어도 항상 올바른 ID를 보장한다).
            val pots = snapshot?.documents?.mapNotNull { doc ->
                doc.toObject(PotDto::class.java)
            } ?: emptyList()
            trySend(pots)
        }

        awaitClose { listener.remove() }
    }

    //새로운 화분 추가
    override suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit> =runCatching {
        val newDocRef = db.collection("users").document(uid).collection("pots").document()
        val newPotDto = PotDto(
            id = newDocRef.id,
            tagId = tag.id,
            tagName = tag.name,
            name = name,
            potTotalStudyingTime = 0L,
            createdAt = null,
            completedAt = null,
            isCompleted = false
        )
        newDocRef.set(newPotDto).await()
    }

    // 화분 이름 변경 — 기다리지 않고 Task를 그대로 돌려준다 (기다리는 시간은 Repository가 정함)
    override fun updatePotName(uid: String, potId: String, name: String): Task<Void> =
        db.collection("users").document(uid)
            .collection("pots").document(potId)
            .update("name", name)

    private fun userRef(uid: String) = db.collection("users").document(uid)

    private fun potRef(uid: String, potId: String) = userRef(uid).collection("pots").document(potId)

    override suspend fun getPotPreferringCache(uid: String, potId: String): PotDto? =
        potRef(uid, potId).getPreferringCache()
            .takeIf { it.exists() }
            ?.toObject(PotDto::class.java)

    override suspend fun getPotOnServer(uid: String, potId: String): PotDto? =
        potRef(uid, potId).get(Source.SERVER).await()
            .takeIf { it.exists() }
            ?.toObject(PotDto::class.java)

    override fun completePot(uid: String, potId: String): Task<Void> =
        db.runBatch { batch ->
            batch.update(
                potRef(uid, potId),
                mapOf(
                    "isCompleted" to true,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            )
            // 규칙이 완료 수 +1이 어느 화분 때문인지 확인하도록 화분 ID를 함께 적는다
            batch.update(
                userRef(uid),
                mapOf(
                    "completedPotsCount" to FieldValue.increment(1),
                    "lastCompletedPotId" to potId
                )
            )
        }

    override fun deletePot(uid: String, potId: String, potTotalStudyingTime: Long, isCompleted: Boolean): Task<Void> {
        // 규칙이 차감이 어느 화분 때문인지 확인하도록 화분 ID를 함께 적는다 (lastDeletedLogId = "": 기록 삭제가 아닌 화분 삭제)
        val userUpdates = mutableMapOf<String, Any>(
            "totalStudyTime" to FieldValue.increment(-potTotalStudyingTime),
            "lastDeletedPotId" to potId,
            "lastDeletedLogId" to ""
        )
        if (isCompleted) userUpdates["completedPotsCount"] = FieldValue.increment(-1)
        return db.runBatch { batch ->
            batch.delete(potRef(uid, potId))
            batch.update(userRef(uid), userUpdates)
        }
    }

    // 화분 목록 일회 조회 (리포트용) - 완료 화분 포함
    override suspend fun getPotsOnce(uid: String): GetPotsOnceResult {
        val snapshot = db.collection("users").document(uid)
            .collection("pots")
            .get()
            .await()
        return GetPotsOnceResult(
            pots = snapshot.documents.mapNotNull { it.toObject(PotDto::class.java) },
            isFromCache = snapshot.metadata.isFromCache
        )
    }

    override suspend fun getTodayStudyLogs(uid: String, potId: String, startOfToday: Date): List<StudyLogDto> {
        val snapshot = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .collection("logs")
            .whereGreaterThanOrEqualTo("startedAt", startOfToday)
            .get()
            .await()

        return snapshot.documents.mapNotNull { doc ->
            doc.toObject(StudyLogDto::class.java)
        }
    }

    override fun levelUpPot(uid: String, potId: String, newLevel: Int, items: Map<ItemType, Int>): Task<Void> =
        db.runBatch { batch ->
            // 서버 레벨과 비교할 수 있도록 증가값이 아닌 정해진 레벨을 쓴다 (같은 레벨업이 두 번 도착하면 두 번째는 거부됨)
            batch.update(potRef(uid, potId), "potLevel", newLevel)
            // 규칙이 아이템 차감이 어느 화분의 레벨업 때문인지 확인하도록 화분 ID를 함께 적는다
            val userUpdates = items.entries.associate { (type, count) ->
                "item.${type.fieldKey}" to FieldValue.increment(-count.toLong())
            } + ("lastLevelUpPotId" to potId)
            batch.update(userRef(uid), userUpdates)
        }
}
