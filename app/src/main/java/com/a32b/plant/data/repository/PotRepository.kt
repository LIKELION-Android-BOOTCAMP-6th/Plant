package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.di.CurrentUser.uid
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.model.Tag
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.jvm.java

class PotRepository(private val db: FirebaseFirestore) {

    //태그 획득
    fun getAvailableTags(): Flow<List<Tag>> = callbackFlow {
        val collectionRef = db.collection("Tags").orderBy("no")

        // 데이터 변경 감지
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error) // 에러 발생 -> 스트림 닫기
                return@addSnapshotListener
            }
            val tags = snapshot?.toObjects( Tag::class.java) ?: emptyList()

            //data 전송
            trySend(tags)
        }
        // 메모리 누수 방지
        awaitClose { listener.remove() }
    }

    // 새 화분 DB에 추가
    suspend fun addPot(uid: String, tag: Tag, name: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            //ID 먼저 생성 -> 아직 생성되지 않은 ID 값을 어떻게 가져올 수 있을지 고민함.
            val newDocRef = db.collection("users").document(uid).collection("pots").document()

            // 기본 값 저장
            val newPot = PotInfo(
                id = newDocRef.id,
                tag_id = tag.id,
                tag_name = tag.name,
                name = name,
                imageUrl = "0", //⭐⭐⭐일단 데이터클래스랑 맞춰놨는데, 디비에는 레벨로 표현되어 있어서 데이터클래스를 바꿔야 함
                potTotalStudyingTime = 0L
            )
            Log.d("plantLog", "--------------------------------")
            Log.d("plantLog", "$newPot")
            Log.d("plantLog", "--------------------------------")
            //DB에 저장
            newDocRef.set(newPot)
                .addOnSuccessListener {
                    cont.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    cont.resume(Result.failure(e))
                }
        }


    suspend fun getDuplicationLevelList(uid: String): List<String> {
        val result = db.collection("users").document(uid).collection("pots")
            .get()
            .await()
        val levelList = result.documents
            .mapNotNull { document -> document.getString("level") }
        val resultList = levelList.distinct().sorted()
        return resultList
    }

    fun createStudyLog(potId: String, studyLog: StudyLog) {
        val docRef = db.collection("users").document(CurrentUser.uid)
            .collection("pots").document(potId)
            .collection("logs").document()
        docRef.set(studyLog.copy(id = docRef.id))
            .addOnSuccessListener {
                Log.d("스터디로그", "성공적")
            }
    }

    fun updateTotalStudyTime(potId: String, studyTime: Long) {
        db.collection("users").document(CurrentUser.uid)
            .collection("pots").document(potId)
            .update("potTotalStudyingTime", FieldValue.increment(studyTime))
    }

    suspend fun getUserPotsByStatus(uid: String, isCompleted: Boolean): List<PotInfo> {
        return db.collection("users").document(uid).collection("pots")
            .whereEqualTo("isCompleted", isCompleted)
            .get().await().toObjects(PotInfo::class.java)
    }

    suspend fun getUserPotById(uid: String, potId: String): PotInfo? {
        return try {
            val result = db.collection("users")
                .document(uid)
                .collection("pots")
                .document(potId) // 문서 ID로 직접 접근
                .get()
                .await()
            result.toObject(PotInfo::class.java)

        } catch (e: Exception) {
            Log.e("plantLog", "${e.message}")
            null
        }
    }

    suspend fun getPotLogs(uid: String, potId: String): List<StudyLog> {
        return try {
            val result = db.collection("users")
                .document(uid)
                .collection("pots")
                .document(potId)
                .collection("logs")
                .orderBy("createAt", Query.Direction.DESCENDING) // 내림차순
                .get().await()
            result.toObjects(StudyLog::class.java)
        } catch (e: Exception) {
            Log.e("plantLog", "${e.message}")
            emptyList()
        }
    }

    //선택된 학습 기록 내용 조회
    suspend fun getSelectedStudyLog(potId: String, logId: String): StudyLog?{
        return try {
            db.collection("users").document(uid)
                .collection("pots").document(potId)
                .collection("logs").document(logId)
                .get()
                .await()
                .toObject(StudyLog::class.java)
        }catch (e: Exception) {
            Log.e("selectedStudyLog", "${e.message}")
            null
        }

    }

    fun updatePotLevelOnly(potId: String, newLevel: String){
        val uid = CurrentUser.uid
        if(uid.isEmpty()) return

        val updates = mapOf(
            "imageUrl" to newLevel,
            "level" to newLevel
        )
        db.collection("users").document(uid)
            .collection("pots").document(potId)
            .update(updates)
            .addOnSuccessListener {
                Log.d("LevelSync", "화분($potId) DB 레벨이 ${newLevel}로 동기화되었습니다.")
            }
            .addOnFailureListener { e ->
                Log.e("LevelSync", "레벨 업데이트 실패", e)
            }
    }


}
