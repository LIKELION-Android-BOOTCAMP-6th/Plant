package com.a32b.plant.data.source.remote.community

import android.util.Log
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.data.model.CommentDto
import com.a32b.plant.data.model.CommunityActivityDto
import com.a32b.plant.data.model.PostDto
import com.a32b.plant.data.model.TagDto
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.type.ActivityType
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth
): CommunityRemoteDataSource {

    override suspend fun loadPostPage(
        cursor: Timestamp?,
        pageSize: Int,
        tagIds: List<String>,
        sharedOnly: Boolean
    ): List<PostDto> {
        var query: com.google.firebase.firestore.Query = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (sharedOnly) {
            query = query.whereEqualTo("isShared", true)
        }
        if (tagIds.isNotEmpty()) {
            query = query.whereIn("tag.id", tagIds)
        }
        if (cursor != null) {
            query = query.startAfter(cursor)
        }
        query = query.limit((pageSize + 1).toLong())

        val snapshot = query.get().await()
        // isFromCache=true이면서 비어있음 → 오프라인 상태라 빈 캐시를 즉시 반환한 것
        // (온라인이면 isFromCache=false, 오프라인+데이터 있으면 isFromCache=true but not empty)
        if (snapshot.metadata.isFromCache && snapshot.isEmpty) throw AppError.Network()

        return snapshot.documents.mapNotNull { doc ->
            safeRunCatching { doc.toObject(PostDto::class.java) }
                .onFailure { e -> Log.e("CommunityRemoteDS", "게시글 파싱 실패: ${doc.id}", e) }
                .getOrNull()
        }
    }

    override suspend fun loadAllPosts(
        tagIds: List<String>,
        sharedOnly: Boolean
    ): List<PostDto> {
        var query: com.google.firebase.firestore.Query = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)

        if (sharedOnly) {
            query = query.whereEqualTo("isShared", true)
        }
        if (tagIds.isNotEmpty()) {
            query = query.whereIn("tag.id", tagIds)
        }
        // limit 없음 → 전체 fetch

        val snapshot = query.get().await()
        if (snapshot.metadata.isFromCache && snapshot.isEmpty) throw AppError.Network()

        return snapshot.documents.mapNotNull { doc ->
            safeRunCatching { doc.toObject(PostDto::class.java) }
                .onFailure { e -> Log.e("CommunityRemoteDS", "게시글 파싱 실패: ${doc.id}", e) }
                .getOrNull()
        }
    }

    override suspend fun getPostDetail(postId: String): PostDto? {
        return db.collection("posts").document(postId)
            .get()
            .await()
            .toObject(PostDto::class.java)
    }

    override suspend fun getComments(postId: String): List<CommentDto> {
        return db.collection("posts").document(postId)
            .collection("comments")
            // 정렬 방식 : 최신이 맨 아래로 가게
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .await()
            .documents.mapNotNull { doc -> doc.toObject(CommentDto::class.java) }
    }

    override suspend fun getTags(): List<TagDto> {
        val order = listOf("중등", "고등", "대학교", "취업준비", "자기계발")
        return db.collection("Tags")
            .orderBy("no")
            .get()
            .await()
            .toObjects(TagDto::class.java)
            .sortedBy { order.indexOf(it.id) }
    }

    override suspend fun savePost(post: PostDto, activity: CommunityActivityDto): String {
        val postRef = db.collection("posts").document()
        val activityRef = db.collection("activities").document()

        val postWithAct = post.copy(activityId = activityRef.id)
        val activityWithPost = activity.copy(targetId = postRef.id)

        db.runBatch { batch ->
            batch.set(postRef, postWithAct)
            batch.set(activityRef, activityWithPost)
        }.await()

        return postRef.id
    }

    override suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String?, tag: TagDto?) {
        if (isShared) {
            db.collection("posts").document(postId)
                .update(
                    "title", title,
                    "updatedAt", Timestamp.now()
                )
                .await()
        } else {
            db.collection("posts").document(postId)
                .update(
                    "title", title,
                    "content", content,
                    "tag", tag,
                    "updatedAt", Timestamp.now() // createdAt은 최초 작성 시점 그대로 유지
                )
                .await()
        }
    }

    override suspend fun deletePost(postId: String) {
        val postRef = db.collection("posts").document(postId)

        val comments = postRef.collection("comments").get().await()
        for (commentDoc in comments.documents) {
            commentDoc.reference.delete().await()
        }

        postRef.delete().await()
    }

    override suspend fun toggleLike(postId: String, uid: String): Boolean {
        val postRef = db.collection("posts").document(postId)

        // 트랜잭션 안에서 likedBy를 직접 읽어 좋아요/취소 방향을 서버가 자체 결정
        // → 다른 기기에서 상태가 바뀌어도 잘못된 방향으로 flip되지 않음
        return db.runTransaction { txn ->
            val snapshot = txn.get(postRef)
            @Suppress("UNCHECKED_CAST")
            val likedBy = (snapshot.get("likedBy") as? List<String>) ?: emptyList()
            val already = uid in likedBy

            if (already) {
                txn.update(postRef, mapOf(
                    "likedBy" to FieldValue.arrayRemove(uid),
                    "likeCount" to FieldValue.increment(-1L)
                ))
            } else {
                txn.update(postRef, mapOf(
                    "likedBy" to FieldValue.arrayUnion(uid),
                    "likeCount" to FieldValue.increment(1L)
                ))
            }
            already
        }.await()
    }

    override suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity) {
        val commentRef = db.collection("posts").document(postId)
            .collection("comments").document()
        val activityRef = db.collection("activities").document()

        val commentWithAct = comment.copy(activityId = activityRef.id, commentId = commentRef.id)
        val activityWithCo = activity.copy(targetId = postId, commentId = commentRef.id)

        val postRef = commentRef.parent.parent!!
        db.runBatch { batch ->
            batch.set(commentRef, commentWithAct.toDto())
            batch.set(activityRef, activityWithCo.toDto())
            batch.update(postRef, "commentCount", FieldValue.increment(1))
        }.await()
    }

    override suspend fun updateComment(postId: String, commentId: String, newContent: String) {
        db.collection("posts").document(postId)
            .collection("comments").document(commentId)
            .update("content", newContent)
            .await()
    }

    override suspend fun updateActivityComment(commentId: String, content: String) {
        db.collection("activities")
            .whereEqualTo("commentId", commentId)
            .get()
            .await()
            .documents
            .firstOrNull()?.reference?.update("comment", content)?.await()
    }

    override suspend fun deleteComment(postId: String, commentId: String): String? {
        val commentRef = db.collection("posts").document(postId)
            .collection("comments").document(commentId)
        val postRef = commentRef.parent.parent!!

        // exists() 체크 → delete() → commentCount 감소를 트랜잭션으로 묶어 TOCTOU 방지 + 포스트 부재 시 NOT_FOUND 방어
        return db.runTransaction { txn ->
            val commentSnapshot = txn.get(commentRef)
            if (!commentSnapshot.exists()) return@runTransaction null

            val postSnapshot = txn.get(postRef)
            val actId = commentSnapshot.getString("activityId")
            txn.delete(commentRef)
            if (postSnapshot.exists()) {
                txn.update(postRef, "commentCount", FieldValue.increment(-1))
            }
            actId
        }.await()
    }

    override suspend fun updateActivityTitle(postId: String, title: String) {
        db.collection("activities")
            .whereEqualTo("targetId", postId)
            .whereEqualTo("type", ActivityType.POST)
            .get()
            .await()
            .documents
            .firstOrNull()?.reference?.update("title", title)?.await()
    }

    override fun observeActivity(uid: String, selected: String): Flow<List<CommunityActivityDto>> = callbackFlow {
        val listener = db.collection("activities")
            .whereEqualTo("uid", uid)
            .whereEqualTo("type", selected)
            .orderBy("createAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null){
                    Log.e("커뮤니티 활동", "구독 종료", exception)
                    close(exception)
                    return@addSnapshotListener
                }

                val activities = snapshots?.documents?.mapNotNull { doc ->
                    safeRunCatching { doc.toObject(CommunityActivityDto::class.java) }.getOrNull()
                } ?: emptyList()

                trySend(activities)
            }

        awaitClose { listener.remove() }

    }

    override suspend fun deleteActivity(activityId: String) {
        db.collection("activities")
            .document(activityId)
            .delete()
            .await()
    }

    override suspend fun deleteActivityByType(uid: String, targetId: String, type: String) {
        db.collection("activities")
            .whereEqualTo("uid", uid)
            .whereEqualTo("targetId", targetId)
            .whereEqualTo("type", type)
            .get()
            .await()
            .documents
            .forEach { it.reference.delete().await() }
    }

    override suspend fun createActivity(activity: CommunityActivity) {
        db.collection("activities").add(activity.toDto()).await()
    }

    override suspend fun toggleBookmark(postId: String, uid: String): Boolean {
        val postRef = db.collection("posts").document(postId)

        return db.runTransaction { txn ->
            val snapshot = txn.get(postRef)
            @Suppress("UNCHECKED_CAST")
            val bookmarkedBy = (snapshot.get("bookmarkedBy") as? List<String>) ?: emptyList()
            val already = uid in bookmarkedBy

            if (already) {
                txn.update(postRef, mapOf(
                    "bookmarkedBy" to FieldValue.arrayRemove(uid),
                    "bookmarkCount" to FieldValue.increment(-1L)
                ))
            } else {
                txn.update(postRef, mapOf(
                    "bookmarkedBy" to FieldValue.arrayUnion(uid),
                    "bookmarkCount" to FieldValue.increment(1L)
                ))
            }
            already
        }.await()
    }

    override suspend fun findCommentIdByActivityId(postId: String, activityId: String): String? {
        val snapshot = db.collection("posts").document(postId)
            .collection("comments")
            .whereEqualTo("activityId", activityId)
            .limit(1)
            .get()
            .await()
        return snapshot.documents.firstOrNull()?.id
    }

    override suspend fun isPostExist(postId: String): Boolean = db.collection("posts").document(postId).get().await().exists()
}
