package com.a32b.plant.data.repository

import com.a32b.plant.data.model.PotInfo
import android.util.Log
import androidx.compose.runtime.retain.retain
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.di.UserModel
import com.google.firebase.firestore.FirebaseFirestore
import com.a32b.plant.data.model.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ListenerRegistration
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.tasks.await
import kotlin.Boolean
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class UserRepository(private val db: FirebaseFirestore, private val auth: FirebaseAuth) {

    private var listenerRegistration: ListenerRegistration? = null

    fun startUserListener(){
        val uid = auth.currentUser?.uid?:return
        listenerRegistration = db
            .collection("users")
            .document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null || !snapshot.exists()) {
                    CurrentUser.clear()
                    return@addSnapshotListener
                }
                snapshot.toObject(UserModel::class.java)?.let {
                    CurrentUser.set(it) // 전역 상태 업데이트
                }
            }
    }
    fun stopUserListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
        CurrentUser.clear()
    }

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
        fun sendUpdate() {
            trySend(userProfile?.copy(potList = currentOngoingPots))
        }

        // 1. 유저 정보 실시간 리스너
        val userListener = userDocRef.addSnapshotListener { userSnapshot, _ ->
            userProfile = userSnapshot?.toObject(UserProfile::class.java)
            sendUpdate()
        }

        // 2. 화분 목록 실시간 리스너 (화분 추가/삭제 시에도 UI 즉시 반영)
        val potsListener = potsCollectionRef.whereEqualTo("isCompleted", false)
            .addSnapshotListener { potsSnapshot, _ ->
                currentOngoingPots = potsSnapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(PotInfo::class.java)?.copy(id = doc.id)
                } ?: emptyList()
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


        // 2. activities/{activityId} 문서들 삭제 (uid 필드로 조회)
        val activities = db.collection("activities")
            .whereEqualTo("uid", uid).get().await()
        for (activity in activities.documents) {
            activity.reference.delete().await()
        }


        // 3. 내 게시글 + 하위 컬렉션(comments, likes) 삭제
        val myPosts = db.collection("posts")
            .whereEqualTo("author.id", uid).get().await()
        for (post in myPosts.documents) {
            val comments = post.reference.collection("comments").get().await()
            for (comment in comments.documents) {
                comment.reference.delete().await()
            }
            val likes = post.reference.collection("likes").get().await()
            for (like in likes.documents) {
                like.reference.delete().await()
            }
            post.reference.delete().await()
        }


        // 4. 다른 사람 게시글에 남긴 내 댓글 → 소프트 삭제 (내용만 변경)
        val myComments = db.collectionGroup("comments")
            .whereEqualTo("user.uid", uid).get().await()
        Log.d("UserRepository", "내 댓글 수: ${myComments.size()}")  // ★ 추가
        for (comment in myComments.documents) {
            comment.reference.update(
                mapOf(
                    "content" to "- 삭제된 댓글입니다. -",
                    "user" to mapOf(
                        "id" to uid,
                        "nickname" to "(알 수 없음)",
                        "profileImg" to ""
                    )
                )
            ).await()
        }


        // 5. 다른 사람 게시글에 남긴 내 좋아요 삭제
        val allPosts = db.collection("posts").get().await()
        for (post in allPosts.documents) {
            val myLike = post.reference.collection("likes").document(uid).get().await()
            if (myLike.exists()) {
                myLike.reference.delete().await()
            }
        }


        // 6. users/{uid} 문서는 맨 마지막에 삭제 (위 단계 실패 시 유저 데이터 보존)
        db.collection("users").document(uid).delete().await()

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

    // 다크모드 관리용
    // 테마 다크모드용 Flow
    fun getUserFlow(uid: String): Flow<UserProfile?> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(null)
            return@callbackFlow
        }
        val userDocRef = db.collection("users").document(uid)
        // 실시간 리스너 등록, 변경 사항 생길 때마다 알림 (내 앱한테)보내는  구독 상태로 만든 것
        val userListener = userDocRef.addSnapshotListener { userSnapshot, error ->
            // users 데이터 바뀔때마다 여기 실행
            if (error != null) {
                close(error)
                return@addSnapshotListener
            }
            // 받아온 데이터 UserProfile 객체로 변환
            val userProfile = userSnapshot?.toObject(UserProfile::class.java)
            trySend(userProfile)
        }
        // 이 Flow 사용하는 화면 닫힐 때 리스너 제거
        awaitClose {
            userListener.remove()
        }
    }
}