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
    // 특정 유저의 데이터 + 하위 pots 목록까지 "모두" 실시간으로 감지
    fun getUserProfile(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(null)
            return@callbackFlow
        }
        val userDocRef = db.collection("users").document(uid)
        val potsCollectionRef = userDocRef.collection("pots")

        var userProfile: UserProfile? = null
        var currentOngoingPots: List<PotInfo> = emptyList()

        //공통 전송 로직
        fun sendUpdate(){
            trySend(userProfile?.copy(potList = currentOngoingPots))
        }

        // 1. 유저 정보 실시간 리스너
        val userListener = userDocRef.addSnapshotListener { userSnapshot, _ ->
            userProfile = userSnapshot?.toObject(UserProfile::class.java)
            sendUpdate()
        }

        // 2. 화분 목록 실시간 리스너 (화분 추가/삭제 시에도 UI 즉시 반영)
        val potsListener = potsCollectionRef.whereEqualTo("completed", false)
            .addSnapshotListener{ potsSnapshot, _ ->
            currentOngoingPots = potsSnapshot?.documents?.mapNotNull{ doc ->
                doc.toObject(PotInfo::class.java)?.copy(id = doc.id)
            }?: emptyList()
            sendUpdate()
        }

        awaitClose {
            userListener.remove()
            potsListener.remove()
        }
    }

    // 유저 프로필 1회성 가져오기 (로그인 시 isFirstLogin 체크용)
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
                isFirstLogin = true,  // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 설정하면 false
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
                "isFirstLogin", false
            )
            .await()
    }

    // 유저 닉네임, 프로필 이미지 레벨 업데이트
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

    // 회원탈퇴 시 유저 관련 모든 데이터 삭제
    suspend fun deleteUser(uid: String) {

        // 1. users/{uid}/pots/{potId}/logs/ (하위-하위) 컬렉션 내 문서들 삭제
        val pots = db.collection("users").document(uid)
            .collection("pots").get().await()
        for (pot in pots.documents) {
            // {lodId} 문서들 삭제
            val logs = pot.reference.collection("logs").get().await()
            for (log in logs.documents) {
                log.reference.delete().await()
            }
            // {potsId} 문서들 삭제
            pot.reference.delete().await()
        }


        // 2. users/{uid} 문서 삭제
        db.collection("users").document(uid).delete().await()


        // 3. activities/{activityId} 문서들 삭제 (uid 필드로 조회)
        val activities = db.collection("activities")
            .whereEqualTo("uid", uid).get().await()
        for (activity in activities.documents) {
            activity.reference.delete().await()
        }


        // 4. posts 중 내 게시글 삭제 → 하위 컬렉션(comments, likes)도 함께 삭제
        // 게시글들 중 내 게시글만 가져오기 (author.id 필드로 조회)
        val myPosts = db.collection("posts")
            .whereEqualTo("author.id", uid).get().await()

        for (post in myPosts.documents) {
            // 내 게시글 내 comments/{commentsId} 문서들(comments 하위 컬렉션) 전부 삭제
            val comments = post.reference.collection("comments").get().await()
            for (comment in comments.documents) {
                comment.reference.delete().await()
            }
            // 내 게시글 likes/{uid} 문서들(likes 하위 컬렉션) 전부 삭제
            val likes = post.reference.collection("likes").get().await()
            for (like in likes.documents) {
                like.reference.delete().await()
            }
            // {postId} 문서 삭제
            post.reference.delete().await()
        }


        // 5. 다른 사람 게시글에 남긴 내 댓글 삭제 (collectionGroup 이용)
        val myComments = db.collectionGroup("comments")
            .whereEqualTo("user.id", uid).get().await()
        for (comment in myComments.documents) {
            comment.reference.delete().await()
        }


        // 6. 다른 사람 게시글에 남긴 내 좋아요 삭제
        //    likes/{uid} 구조이므로 posts에서 likes/{uid} 문서를 모두 찾아 삭제...
        val allPosts = db.collection("posts").get().await()
        for (post in allPosts.documents) {
            val myLike = post.reference.collection("likes").document(uid).get().await()
            if (myLike.exists()) {
                myLike.reference.delete().await()
            }
        }
    }

    suspend fun getPotId() = "현재 팟 아이디"

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

    // uid 로 users/pots 데이터 가져오기
    suspend fun getUsersPots(uid: String): List<PotInfo> {
        return try {
            db.collection("users").document(uid).collection("pots").get().await()
                .toObjects(PotInfo::class.java)
        } catch (e: Exception) {
            Log.e("error", e.message.toString())
            emptyList()
        }
    }
}

