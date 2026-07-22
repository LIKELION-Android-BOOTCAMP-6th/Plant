package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.datasource.pot.PotRemoteDataSource
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.StudyLog
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

    // user uid 없음 -> 합친 후에 변경 예정
//    override fun getPots(uid: String): Flow<List<PotDto>> = callbackFlow {
//        // 1. uid가 비어있는지 체크 (중요!)
//        if (uid.isEmpty()) {
//            close(Exception("UID is empty"))
//            return@callbackFlow
//        }
//
//        // 2. 정확한 경로 설정: users 컬렉션 -> 특정 uid 문서 -> pots 컬렉션
//        val collectionRef = db.collection("users")
//            .document(uid)
//            .collection("pots")
//
//        val listener = collectionRef.addSnapshotListener { snapshot, error ->
//            if (error != null) {
//                close(error)
//                return@addSnapshotListener
//            }
//
//            val pots = snapshot?.toObjects(PotDto::class.java) ?: emptyList()
//            trySend(pots)
//        }
//
//        awaitClose { listener.remove() }
//    }
    override suspend fun addPot(uid: String, tag: Tag, name: String) =
        potRemoteDataSource.addPot(uid, tag, name)

    override suspend fun updatePotLevel(uid: String, potId: String, newLevel: String) =
        potRemoteDataSource.updatePotLevel(uid, potId, newLevel)

    override fun createStudyLog(potId: String, studyLog: StudyLog) {
        val docRef = db.collection("users").document(CurrentUser.uid)
            .collection("pots").document(potId)
            .collection("logs").document()
        docRef.set(studyLog.copy(id = docRef.id))
    }

    override fun updateTotalStudyTime(potId: String, studyTime: Long) {
        db.collection("users").document(CurrentUser.uid)
            .collection("pots").document(potId)
            .update("potTotalStudyingTime", FieldValue.increment(studyTime))
    }

    //특정 화분 정보 조회
    override suspend fun getUserPotById(uid: String, potId: String): Pot? {
        return db.collection("users").document(uid)
            .collection("pots").document(potId)
            .get().await()?.toObject(Pot::class.java)
    }

    //학습 기록 목록 조회
    override suspend fun getPotLogs(uid: String, potId: String): List<StudyLog> {
        val snapshot = db.collection("users").document(uid)
            .collection("pots").document(potId)
            .collection("logs")
            .orderBy("createAt", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().await()
        return snapshot.toObjects(StudyLog::class.java)
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

    override suspend fun getSelectedStudyLog(potId: String, logId: String): StudyLog? {
        return db.collection("users").document(CurrentUser.uid)
            .collection("pots").document(potId)
            .collection("logs").document(logId)
            .get().await().toObject(StudyLog::class.java)
    }
}