package com.a32b.plant.data.source.remote.user

import android.util.Log
import com.a32b.plant.data.mapper.toDomain
import com.a32b.plant.data.model.UserDto
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.type.ItemType
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
) : UserRemoteDataSource {

    override suspend fun getUser(uid: String): UserDto? {
        val snapshot = db.collection("users").document(uid).get().await()
        return snapshot.toObject(UserDto::class.java)
    }

    override fun observeUser(uid: String): Flow<UserDto?> = callbackFlow {
        if (uid.isEmpty()) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val listener = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(UserDto::class.java))
            }
        awaitClose { listener.remove() }
    }

    override suspend fun createUser(uid: String, user: UserDto) {
        db.collection("users").document(uid).set(user).await()
    }

    override suspend fun completeFirstLogin(uid: String, nickname: String) {
        db.collection("users").document(uid)
            .update(
                "nickname", nickname,
                "isFirstLogin", false
            )
            .await()
    }

    override suspend fun updateDarkMode(uid: String, isDarkMode: Boolean) {
        db.collection("users").document(uid)
            .update("isDarkMode", isDarkMode)
            .await()
    }

    override suspend fun updateProfile(
        uid: String,
        nickname: String,
        profileImg: String
    ) {
        db.collection("users")
            .document(uid)
            .update(
                "nickname", nickname,
                "profileImg", profileImg
            )
            .await()
    }

    override suspend fun isNicknameTaken(nickname: String): Boolean {
        val doc = db.collection("nicknames").document(nickname).get().await()
        return doc.exists()
    }

    override suspend fun registerNickname(nickname: String) {
        db.collection("nicknames")
            .document(nickname)
            .set(mapOf("nickname" to nickname))
            .await()
    }

    override suspend fun deleteNickname(nickname: String) {
        db.collection("nicknames").document(nickname).delete().await()
    }

    override suspend fun checkAttendance(uid: String): AttendanceDecision {
        val userRef = db.collection("users").document(uid)

        return db.runTransaction { txn ->
            val snapshot = txn.get(userRef)
            val dto = snapshot.toObject(UserDto::class.java)
                ?: throw AppError.UnknownUser()

            val nowKst = LocalDate.now(ZoneId.of("Asia/Seoul"))
            val decision = dto.toDomain().monthCheck.decideNext(nowKst)

            if (decision is AttendanceDecision.Success) {
                val updates = mutableMapOf<String, Any>(
                    "dailyCheckThisMonth.count" to decision.newCount,
                    "dailyCheckThisMonth.lastCheckedAt" to FieldValue.serverTimestamp()
                )

                when (val reward = decision.reward) {
                    is AttendanceReward.Coin ->
                        updates["coin"] = FieldValue.increment(reward.amount.toLong())

                    is AttendanceReward.ItemReward -> {
                        val type = reward.type

                        check(
                            type != ItemType.GOLD_300 &&
                                    type != ItemType.GOLD_500 &&
                                    type != ItemType.GOLD_1000
                        ) {
                            "출석 아이템 보상에 골드 타입은 사용할 수 없습니다: $type"
                        }

                        val fieldKey = requireNotNull(type.fieldKey) {
                            "출석 아이템 보상의 Firestore 필드 키가 없습니다: $type"
                        }

                        updates["item.$fieldKey"] =
                            FieldValue.increment(reward.amount.toLong())
                    }

                    null -> Unit
                }

                txn.update(userRef, updates)
            }

            decision
        }.await()
    }

    override suspend fun deleteUserData(uid: String) {
        Log.d("UserRemoteDataSource", "deleteUserData 시작: uid=$uid")

        // 1. users/{uid}/pots/{potId}/logs/ (하위-하위) 컬렉션 내 문서들 삭제
        Log.d("UserRemoteDataSource", "Step 1: pots 삭제 시작")
        val pots = db.collection("users").document(uid)
            .collection("pots").get().await()
        for (pot in pots.documents) {
            val logs = pot.reference.collection("logs").get().await()
            for (log in logs.documents) {
                log.reference.delete().await()
            }
            pot.reference.delete().await()
        }
        Log.d("UserRemoteDataSource", "Step 1 완료: pots ${pots.size()}개")

        // 2. activities/{activityId} 문서들 삭제 (uid 필드로 조회)
        Log.d("UserRemoteDataSource", "Step 2: activities 삭제 시작")
        val activities = db.collection("activities")
            .whereEqualTo("uid", uid).get().await()
        for (activity in activities.documents) {
            activity.reference.delete().await()
        }
        Log.d("UserRemoteDataSource", "Step 2 완료: activities ${activities.size()}개")

        // 3. 내 게시글 + 하위 컬렉션(comments, likes) 삭제
        Log.d("UserRemoteDataSource", "Step 3: 내 posts 삭제 시작")
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
        Log.d("UserRemoteDataSource", "Step 3 완료: posts ${myPosts.size()}개")

        // 4. 다른 사람 게시글에 남긴 내 댓글 → 소프트 삭제 (내용만 변경)
        Log.d("UserRemoteDataSource", "Step 4: 댓글 소프트 삭제 시작")
        val myComments = db.collectionGroup("comments")
            .whereEqualTo("user.uid", uid).get().await()
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
        Log.d("UserRemoteDataSource", "Step 4 완료: 댓글 ${myComments.size()}개")

        // 5. 다른 사람 게시글에 남긴 내 좋아요 삭제
        // collectionGroup + FieldPath.documentId()는 단순 uid를 경로로 인식하지 못해 사용 불가.
        // posts의 likedBy 배열로 내가 좋아요한 게시글을 조회 후 likes/{uid} 문서를 직접 삭제.
        Log.d("UserRemoteDataSource", "Step 5: 내 좋아요 삭제 시작")
        val likedPosts = db.collection("posts")
            .whereArrayContains("likedBy", uid)
            .get().await()
        for (post in likedPosts.documents) {
            post.reference.collection("likes").document(uid).delete().await()
        }
        Log.d("UserRemoteDataSource", "Step 5 완료: 좋아요 ${likedPosts.size()}개")

        // 6. users/{uid} 문서는 맨 마지막에 삭제 (위 단계 실패 시 유저 데이터 보존)
        Log.d("UserRemoteDataSource", "Step 6: users 문서 삭제")
        db.collection("users").document(uid).delete().await()
        Log.d("UserRemoteDataSource", "deleteUserData 완료")
    }
}
