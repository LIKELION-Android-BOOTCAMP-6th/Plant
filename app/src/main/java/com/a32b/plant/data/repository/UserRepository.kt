package com.a32b.plant.data.repository

import com.a32b.plant.data.model.PotInfo
import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.a32b.plant.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.Boolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {
    // 특정 유저의 데이터를 실시간 Flow로 반환
    // 특정 유저의 데이터 + 하위 pots 목록까지 "모두" 실시간으로 감치
    fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(null)
            return@callbackFlow
        }
        val userDocRef = db.collection("users").document(uid)
        val potsCollectionRef = userDocRef.collection("pots")

        var userProfile: UserProfile? = null

        // 1. 유저 정보 실시간 리스너
        val userListener = userDocRef.addSnapshotListener { userSnapshot, _ ->
            userProfile = userSnapshot?.toObject(UserProfile::class.java)

            // 유저 정보가 오면 화분 목록도 쿼리해서 전송
            potsCollectionRef.get().addOnSuccessListener { potsSnapshot ->
                val pots = potsSnapshot.toObjects(PotInfo::class.java)
                trySend(userProfile?.copy(potList = pots))
            }
        }

        // 2. 화분 목록 실시간 리스너 (화분 추가/삭제 시에도 UI 즉시 반영)
        val potsListener = potsCollectionRef.addSnapshotListener { potsSnapshot, _ ->
            val pots = potsSnapshot?.toObjects(PotInfo::class.java) ?: emptyList()
            if (userProfile != null) {
                trySend(userProfile?.copy(potList = pots))
            }
        }

        awaitClose {
            userListener.remove()
            potsListener.remove()
        }
    }

    // 유저 프로필 1회성 가져오기 (로그인 시 isFirstLogin 체크 등)
    suspend fun getUserProfileOnce(uid: String): UserProfile? {
        return try {
            val snapshot = db.collection("users").document(uid).get().await()
            snapshot.toObject(UserProfile::class.java)
        } catch (e: Exception) {
            Log.e("UserRepository", "getUserProfileOnce 실패: ${e.message}", e)
            null
        }
    }

    // suspendCancellableCoroutine , 유저 정보 생성
    suspend fun createUser(uid: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val newUser = UserProfile(
                // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 재설정 하고 false 바꾸기
                isFirstLogin = true,
                isAutoLogin = false,
                isDarkMode = false,
                totalStudyTime = 0L,
                completedPotsCount = 0
            )
            db.collection("users")
                .document(uid)
                .set(newUser)
                .addOnSuccessListener {
                    cont.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    cont.resume(Result.failure(e))
                }
        }

    // 첫 로그인 완료 처리 (닉네임 저장 + isFirstLogin → false + isAutoLogin → true)
    suspend fun completeFirstLogin(uid: String, nickname: String) {
        db.collection("users").document(uid)
            .update(
                "nickname", nickname,
                "isFirstLogin", false,
                "isAutoLogin", true
            )
            .await()
    }

    // 유저 닉네임, 고른 대표 식물 이미지 업데이트
    suspend fun updateNicknameAndImage(uid: String, nickname: String, imageLevel: String) {
        try {
            db.collection("users").document(uid)
                .update(
                    "nickname", nickname,
                    "profileImg", imageLevel
                )
                .addOnSuccessListener { Log.d("UserRepository", "updateNicknameAndImage 업데이트 성공") }
                .addOnFailureListener { e ->
                    Log.w(
                        "UserRepository",
                        "updateNicknameAndImage Error updating document",
                        e
                    )
                }.await()
        } catch (e: Exception) {
            Log.e("error", e.message.toString())
        }
    }


    // 자동 로그인 여부 Firestore에 저장
    suspend fun updateAutoLogin(uid: String, isAutoLogin: Boolean) {
        try {
            db.collection("users").document(uid)
                .update("isAutoLogin", isAutoLogin)
                .await()
        } catch (e: Exception) {
            Log.e("UserRepository", "updateAutoLogin 실패: ${e.message}", e)
        }
    }

    suspend fun getPotId() = "현재 팟 아이디"

    // ********************** push 할 때 autoLogin true 만들기
    fun isAutoLogin() = true
//     fun isAutoLogin() = false

    // 마지막으로 선택한 화분의 ID를 Firestore에 업데이트합니다.
    suspend fun updateLastSelectedPot(uid: String, potId: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            db.collection("users")
                .document(uid)
                .update("lastSelectedPotId", potId) // 특정 필드만 업데이트
                .addOnSuccessListener {
                    cont.resume(Result.success(Unit))
                }
                .addOnFailureListener { e ->
                    // 실패 시 에러와 함께 resume
                    cont.resume(Result.failure(e))
                }

        }

    // 다크 모드 토글 버튼 클릭 시 사용자 isDarkMode 업데이트
    suspend fun updateIsDarkMode(uid: String, state: Boolean) {
        try {
            Log.d("plantLog", "----------4 $uid , $state")
            db.collection("users")
                .document(uid)
                .update("isDarkMode", state).await()
        } catch (e: Exception) {
            Log.e("error", e.message.toString())
        }
    }

}

