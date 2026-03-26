package com.a32b.plant.data.repository

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class NicknameRepository(private val db: FirebaseFirestore) {

    // 닉네임 중복 여부 확인
    suspend fun isNicknameTaken(nickname: String): Boolean {
        return try {
            val doc = db.collection("nicknames").document(nickname).get().await()
            doc.exists()
        } catch (e: Exception) {
            Log.e("NicknameRepository", "닉네임 중복 검사 실패: ${e.message}", e)
            // 에러 시 안전하게 중복으로 처리 (재시도 유도)
            true
        }
    }

    // 닉네임 등록 (문서 ID = 닉네임, 필드에도 nickname 저장)
    suspend fun registerNickname(nickname: String) {
        db.collection("nicknames")
            .document(nickname)
            .set(mapOf("nickname" to nickname))
            .await()
    }

    // 닉네임 삭제 (닉네임 변경 시 기존 닉네임 해제용)
    suspend fun deleteNickname(nickname: String) {
        try {
            db.collection("nicknames").document(nickname).delete().await()
        } catch (e: Exception) {
            Log.e("NicknameRepository", "닉네임 삭제 실패: ${e.message}", e)
        }
    }
}