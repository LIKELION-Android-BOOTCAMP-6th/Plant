package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.di.CurrentUser.uid
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.model.LogInfo
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume
import kotlin.jvm.java

class PotRepository(private val db: FirebaseFirestore) {

    //태그 획득
    fun getAvailableTags(): Flow<List<String>> = callbackFlow {
        val collectionRef = db.collection("tags").orderBy("id").limit(5)

        // 데이터 변경 감지
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error) // 에러 발생 -> 스트림 닫기
                return@addSnapshotListener
            }
            //name 필드값 추출 -> 리스트화
            val tags = snapshot?.documents?.mapNotNull { doc -> doc.id } ?: emptyList()

            //data 전송
            trySend(tags)
        }
        // 메모리 누수 방지
        awaitClose { listener.remove() }
    }

    // 새 화분 DB에 추가
    suspend fun addPot(uid: String, tag: String, name: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            //ID 먼저 생성 -> 아직 생성되지 않은 ID 값을 어떻게 가져올 수 있을지 고민함.
            val newDocRef = db.collection("users").document(uid).collection("pots").document()

            // 기본 값 저장
            val newPot = PotInfo(
                id = newDocRef.id,
                tag = tag,
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


    //ARnkLKJE60MuhYMgivXweboI6ch2
    //VosJjoUJp6SplH0siKyoAIBZ7fk2
// 이상 없으면 오전에 물어보고 model에 넣기
// https://stackoverflow.com/questions/48474957/how-to-add-a-timestamp-in-firestore-with-android/48475027#48475027
// https://oneuptime.com/blog/post/2026-02-02-kotlin-firebase/view

    //    "timestamp" to FieldValue.serverTimestamp()
    suspend fun createPot() {
        val pot = PotInfo(
            //사용자의 하위 컬렉션으로 화분 생성 -> uid 별도 기입 불필요
            // 테스트용인걸 알지만 오류가 나서 불가피하게 주석 처리
            //uid = "VosJjoUJp6SplH0siKyoAIBZ7fk2",
            tag = "자격증",
            name = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
            potTotalStudyingTime = 0L,
            isCompleted = false
        )

        val log = StudyLog(
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
            contents = listOf(
                "학습내용abcdabcdefghiabcdabcdefghiabcdabcdefghi",
                "학습내용5555555555555555555 5 5 55 55 5  5",
                "abcdabcdefghiabcdabcdefghiabcdabcdefghi",
                "ddddddddddsdfsdfsdf",
                ""
            ),
            studyingTime = 10L
        )

        db.collection("pots")
            .add(pot)
            .addOnSuccessListener { document ->
                val potDocId = document.id
                Log.d("PlantLog", "생성 성공, ID : $potDocId")
                db.collection("pots")
                    .document(potDocId)
                    .collection("logs").add(log)
                    .addOnSuccessListener {
                        Log.d("PlantLog", "logs add 완료")
                    }
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
        db.collection("users").document(CurrentUser.uid)
            .collection("pots").document(potId)
            .collection("logs")
            .add(studyLog)
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

    suspend fun getPotLogs(uid: String, potId: String): List<LogInfo> {
        return try {
            val result = db.collection("users")
                .document(uid)
                .collection("pots")
                .document(potId)
                .collection("logs")
                .get().await()
            result.toObjects(LogInfo::class.java)
        } catch (e: Exception) {
            Log.e("plantLog", "${e.message}")
            emptyList()
        }
    }


}
