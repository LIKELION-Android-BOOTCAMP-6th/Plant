package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.source.remote.pot.PotRemoteDataSource
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.PotRepository
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PotRepositoryImpl @Inject constructor(
    private val potRemoteDataSource: PotRemoteDataSource,
    private val db: FirebaseFirestore
) : PotRepository {

    override fun getPots(uid: String): Flow<List<Pot>> =
        potRemoteDataSource.getPots(uid)
            .map { dtoList -> dtoList.map { it.toDomain() } }
            // 로그아웃 등으로 인증 토큰이 무효화된 직후에도 리스너가 잠깐 살아있을 수 있다.
            // 이때 서버가 PERMISSION_DENIED를 반환하는데, 처리하지 않으면 앱 전체가 죽는다.
            .catch { e -> Log.e("PotRepository", "화분 목록 구독 실패: ${e.message}", e) }

    override suspend fun addPot(uid: String, tag: Tag, name: String) =
        potRemoteDataSource.addPot(uid, tag, name)

    override suspend fun updatePotLevel(uid: String, potId: String, newLevel: String) =
        potRemoteDataSource.updatePotLevel(uid, potId, newLevel)

    //특정 화분 정보 조회
    override suspend fun getUserPotById(uid: String, potId: String): Pot? {
        return db.collection("users").document(uid)
            .collection("pots").document(potId)
            .get().await()?.toObject(Pot::class.java)
    }

    //중복 제거된 레벨 리스트 조회
    override suspend fun getDuplicationLevelList(uid: String): List<String> {
        val result = db.collection("users").document(uid).collection("pots")
            .get().await()
        return result.documents
            .mapNotNull { it.getString("level") }
            .distinct()
            .sorted()
    }

    override suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<Pot> {
        val snapshot = db.collection("users").document(uid)
            .collection("pots")
            .whereEqualTo("isCompleted", isCompleted)
            .get()
            .await()
        return snapshot.toObjects(Pot::class.java)
    }

    override fun getAvailableTags(): Flow<List<Tag>> = callbackFlow {
        val collectionRef = db.collection("Tags").orderBy("no")
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val tags = snapshot?.toObjects(Tag::class.java) ?: emptyList()
            trySend(tags)
        }
        awaitClose { listener.remove() }
    }

    override suspend fun updatePotName(uid: String, potId: String, newName: String) {
        db.collection("users").document(uid)
            .collection("pots").document(potId)
            .update("name", newName)
            .await()
    }

    override suspend fun deleteEntirePot(uid: String, potId: String, totalStudyingTime: Long) {
        val userRef = db.collection("users").document(uid)
        val potRef = userRef.collection("pots").document(potId)

        db.runBatch { batch ->
            batch.update(userRef, "totalStudyTime", FieldValue.increment(totalStudyingTime * -1))
            batch.delete(potRef)
        }.await()
    }

    override suspend fun completeStudyPlan(uid: String, potId: String) {
        val userRef = db.collection("users").document(uid)
        val potRef = userRef.collection("pots").document(potId)

        db.runBatch { batch ->
            batch.update(userRef, "completedPotsCount", FieldValue.increment(1))
            batch.update(
                potRef, mapOf(
                    "isCompleted" to true,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            )
        }.await()
    }
}
