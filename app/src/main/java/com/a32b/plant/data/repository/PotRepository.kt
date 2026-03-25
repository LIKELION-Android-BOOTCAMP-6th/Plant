package com.a32b.plant.data.repository

import com.a32b.plant.data.model.PotInfo
import com.google.firebase.firestore.FirebaseFirestore
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
            val tags = snapshot?.documents?.mapNotNull { it.getString("name") } ?: emptyList()
            val tags = snapshot?.documents?.mapNotNull { it.id }?: emptyList()

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
                level = "0",
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


    suspend fun getDuplicationLevelList(uid: String): List<String> {
        val result = db.collection("pots")
            .whereEqualTo("uid", uid)
            .get()
            .await()
        val levelList = result.documents
            .mapNotNull { document -> document.getString("level") }
        val resultList = levelList.distinct().sorted()
        return resultList
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