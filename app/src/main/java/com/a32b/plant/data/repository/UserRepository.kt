package com.a32b.plant.data.repository

import com.google.firebase.firestore.FirebaseFirestore
import com.a32b.plant.data.model.UserProfile
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class UserRepository(private val db: FirebaseFirestore) {
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

    suspend fun getPotId() = "현재 팟 아이디"
    fun isAutoLogin() = true
}