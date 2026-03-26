package com.a32b.plant.data.repository

import android.R.attr.name
import android.R.attr.tag
import android.util.Log
import com.a32b.plant.data.model.PotInfo
import com.google.firebase.Timestamp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ServerTimestamp
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.coroutines.resume

class PotRepository(private val db: FirebaseFirestore) {

    //태그 획득
    fun getAvailableTags(): Flow<List<String>> = callbackFlow {
        val collectionRef = db.collection("tags")

        // 데이터 변경 감지
        val listener = collectionRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error) // 에러 발생 -> 스트림 닫기
                return@addSnapshotListener
            }
            //name 필드값 추출 -> 리스트화
            //⭐⭐⭐둘 중 하나 지워야 해서 밑에 걸로 살려둠. 작성하신 분이 보고 둘 중 하나 날리기
//            val tags = snapshot?.documents?.mapNotNull { it.getString("name") } ?: emptyList()
            val tags = snapshot?.documents?.mapNotNull { it.id }?: emptyList()

            //data 전송
            trySend(tags)
        }
        // 메모리 누수 방지
        awaitClose { listener.remove() }
    }

    // 새 화분 DB에 추가
    //⭐⭐⭐addPot으로 되어 있는 함수가 두 개라 오류 발생함 둘 중 하나 지우기
    suspend fun addPot2(uid: String, tag: String, name: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            //ID 먼저 생성 -> 아직 생성되지 않은 ID 값을 어떻게 가져올 수 있을지 고민함.
            val newDocRef = db.collection("users").document(uid).collection("pots").document()

            // 기본 값 저장
            val newPot = PotInfo(
                id = newDocRef.id,
                tag = tag,
                name = name,
                imageUrl = "0", //⭐⭐⭐일단 데이터클래스랑 맞춰놨는데, 디비에는 레벨로 표현되어 있어서 데이터클래스를 바꿔야 함
                todayStudyingTime = 0L
            )

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
    data class Pots(
        val uid: String = "",
        val tag: String = "",
        val name: String = "",
        val level: String = "",
        val todayStudyingTime: Long = 0L,
        @ServerTimestamp
        val createdAt: Timestamp? = null,
        val completedAt: Timestamp? = null,
        val isCompleted: Boolean = false
    )

    data class Logs(
        val title: String = "",
        val content: List<String> = emptyList(),
        val studyTime: Long = 0L,
        @ServerTimestamp
        val createdAt: Timestamp? = null
    )

    //    "timestamp" to FieldValue.serverTimestamp()
    suspend fun createPot() {
        val pot = Pots(
            uid = "VosJjoUJp6SplH0siKyoAIBZ7fk2",
            tag = "자격증",
            name = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
            level = "lv.3",
            todayStudyingTime = 0L,
            isCompleted = false
        )

        val log = Logs(
            title = "Lorem ipsum dolor sit amet, consectetur adipiscing elit",
            content = listOf(
                "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum.",
                "학습내용abcdabcdefghiabcdabcdefghiabcdabcdefghi",
                "학습내용5555555555555555555 5 5 55 55 5  5",
                "abcdabcdefghiabcdabcdefghiabcdabcdefghi",
                "ddddddddddsdfsdfsdf",
                ""
            ),
            studyTime = 10L
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
        val result = db.collection("pots")
            .whereEqualTo("uid", uid)
            .get()
            .await()
        val levelList = result.documents
            .mapNotNull { document -> document.getString("level") }
        val resultList = levelList.distinct().sorted()
        return resultList
    }
    suspend fun addPot(uid: String, tag: String, name: String): Result<Unit> = suspendCancellableCoroutine { cont ->
        //ID 먼저 생성 -> 아직 생성되지 않은 ID 값을 어떻게 가져올 수 있을지 고민함.
        val newDocRef = db.collection("users").document(uid).collection("pots").document()

        // 기본 값 저장
        val newPot = PotInfo(
            id = newDocRef.id,
            tag = tag,
            name = name,
            todayStudyingTime = 0L
        )

        //DB에 저장
        newDocRef.set(newPot)
            .addOnSuccessListener {
                cont.resume(Result.success(Unit))
            }
            .addOnFailureListener { e ->
                cont.resume(Result.failure(e))
            }
    }
}