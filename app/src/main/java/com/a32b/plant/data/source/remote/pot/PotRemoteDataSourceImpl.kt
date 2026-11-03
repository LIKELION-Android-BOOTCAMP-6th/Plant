package com.a32b.plant.data.source.remote.pot

import android.util.Log
import com.a32b.plant.data.model.PotDto
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.type.ItemType
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

    //화분 레벨 업데이트
    override suspend fun updatePotLevel(uid: String, potId: String, newLevel: Int) {
        db.collection("users").document(uid)
            .collection("pots")
            .document(potId)
            .update(mapOf(
                "potLevel" to newLevel
            ))
            .await()
    }

    // 화분 목록 일회 조회 (리포트용) - 완료 화분 포함, 캐시 대신 서버 최신 상태만 사용
    override suspend fun getPotsOnce(uid: String): List<PotDto> {
        val snapshot = db.collection("users").document(uid)
            .collection("pots")
            .get(Source.SERVER)
            .await()
        return snapshot.documents.mapNotNull { it.toObject(PotDto::class.java) }
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

    override suspend fun levelUpWithItemConsumption(
        uid: String,
        potId: String,
        newLevel: Int,
        items: Map<ItemType, Int>
    ) {
        val userRef = db.collection("users").document(uid)
        val potRef = userRef.collection("pots").document(potId)

        db.runTransaction { transaction ->
            val userSnapshot = transaction.get(userRef)

            val rawItemsMap = userSnapshot.get("items") as? Map<*, *> ?: emptyMap<Any, Any>()
            val currentItemsMap = rawItemsMap.entries.associate { (k, v) ->
                (k as? String).orEmpty() to (v as? Number)?.toLong().orElse(0L)
            }

            for ((itemType, requiredCount) in items) {
                val key = itemType.name
                val currentCount = (currentItemsMap[key] ?: 0L).toInt()

                if (currentCount < requiredCount) {
                    throw AppError.InsufficientItems()
                }
            }

            val updatedItemsMap = currentItemsMap.toMutableMap()
            for ((itemType, requiredCount) in items) {
                val key = itemType.name
                val currentCount = (updatedItemsMap[key] ?: 0L).toInt()
                updatedItemsMap[key] = (currentCount - requiredCount).toLong()
            }

            transaction.update(userRef, "items", updatedItemsMap)
            transaction.update(potRef, "potLevel", newLevel)
        }.await()
    }

    private fun Long?.orElse(default: Long): Long = this ?: default
}
