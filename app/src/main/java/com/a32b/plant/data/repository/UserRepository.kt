package com.a32b.plant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.a32b.plant.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(private val db: FirebaseFirestore,private val auth: FirebaseAuth) {
    // 특정 유저의 데이터를 실시간 Flow로 반환
    fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        val docRef = db.collection("users").document(uid)

        val listener = docRef.addSnapshotListener { snapshot, error ->
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            val profile = snapshot?.toObject(UserProfile::class.java)
            trySend(profile)
        }
        awaitClose { listener.remove() }
    }

    // suspendCancellableCoroutine , 유저 정보 생성
    suspend fun createUser(uid: String): Result<Unit> =
        suspendCancellableCoroutine { cont ->
            val newUser = UserProfile(
                // 닉네임 설정 전까지 "",
                nickname = "",
                profileImg = "",
                // isFirstLogin = true 로 로그인 시 닉네임 재설정 하고
                isFirstLogin = true,
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

    suspend fun getPotId() = "현재 팟 아이디"
    fun isAutoLogin() = true
}

