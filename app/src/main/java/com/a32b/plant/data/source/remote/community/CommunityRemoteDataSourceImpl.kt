package com.a32b.plant.data.source.remote.community

import android.util.Log
import com.a32b.plant.data.mapper.toDto
import com.a32b.plant.data.model.CommentDto
import com.a32b.plant.data.model.CommunityActivityDto
import com.a32b.plant.data.model.PostDto
import com.a32b.plant.data.model.TagDto
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.type.ActivityType
import com.google.firebase.Timestamp
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
    private val db: FirebaseFirestore
): CommunityRemoteDataSource {

    override fun getPostList(): Flow<List<PostDto>> = callbackFlow {
        val subscription = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(PostDto::class.java)
                    } catch (e: Exception) {
                        Log.e("CommunityRemoteDataSource", "게시글 파싱 오류: ${doc.id}", e)
                        null
                    }
                } ?: emptyList()

                trySend(posts)
            }
        awaitClose { subscription.remove() }
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

        db.collection("activities").document(getActivityId(postId))
            .update("title", title)
            .await()
    }

    override suspend fun deletePost(postId: String, activityId: String) {
        val postRef = db.collection("posts").document(postId)

        // 1. 하위 컬렉션 comments 문서 삭제
        val comments = postRef.collection("comments").get().await()
        for (commentDoc in comments.documents) {
            commentDoc.reference.delete().await()
        }

        // 2. 게시글 자신의 activity만 삭제 (댓글/좋아요는 남의 활동 기록이라 건드리지 않음)
        deleteActivity(activityId = activityId)

        // 3. 게시글 문서 삭제
        postRef.delete().await()
    }

    override suspend fun toggleLike(postId: String, uid: String, title: String): Boolean {
        val postRef = db.collection("posts").document(postId)

        // 트랜잭션 안에서 likedBy를 직접 읽어 좋아요/취소 방향을 서버가 자체 결정
        // → 다른 기기에서 상태가 바뀌어도 잘못된 방향으로 flip되지 않음
        val wasLiked = db.runTransaction { txn ->
            val snapshot = txn.get(postRef)
            val current = snapshot.getLong("likeCount") ?: 0L
            @Suppress("UNCHECKED_CAST")
            val likedBy = (snapshot.get("likedBy") as? List<String>) ?: emptyList()
            val already = uid in likedBy

            if (already) {
                val newCount = (current - 1).coerceAtLeast(0)
                txn.update(postRef, mapOf(
                    "likedBy" to FieldValue.arrayRemove(uid),
                    "likeCount" to newCount
                ))
            } else {
                txn.update(postRef, mapOf(
                    "likedBy" to FieldValue.arrayUnion(uid),
                    "likeCount" to current + 1
                ))
            }
            already
        }.await()

        if (wasLiked) deleteActivity(uid = uid, targetId = postId, type = ActivityType.LIKE)
        else setLikedActivity(uid, postId, title)

        return wasLiked
    }

    override suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity) {
        val commentRef = db.collection("posts").document(postId)
            .collection("comments").document()
        val activityRef = db.collection("activities").document()

        val commentWithAct = comment.copy(activityId = activityRef.id, commentId = commentRef.id)
        val activityWithCo = activity.copy(targetId = postId, commentId = commentRef.id)

        db.runBatch { batch ->
            batch.set(commentRef, commentWithAct.toDto())
            batch.set(activityRef, activityWithCo.toDto())
        }.await()

        db.collection("posts").document(postId)
            .update("commentCount", FieldValue.increment(1))
            .await()
    }

    override suspend fun updateComment(postId: String, commentId: String, newContent: String) {
        db.collection("posts").document(postId)
            .collection("comments").document(commentId)
            .update("content", newContent)
            .await()

        db.collection("activities").whereEqualTo("commentId", commentId)
            .get()
            .await()
            .documents
            .firstOrNull()?.reference?.update("comment", newContent)?.await()
    }

    override suspend fun deleteComment(postId: String, commentId: String) {
        val commentDoc = db.collection("posts").document(postId)
            .collection("comments").document(commentId)

        // activityId가 있으면 activity도 삭제
        val activityId = commentDoc.get().await().getString("activityId")

        commentDoc.delete().await()

        if (!activityId.isNullOrEmpty()) {
            deleteActivity(activityId = activityId)
        }

        db.collection("posts").document(postId)
            .update("commentCount", FieldValue.increment(-1))
            .await()
    }

    private suspend fun getActivityId(postId: String): String {
        return db.collection("posts").document(postId)
            .get()
            .await()
            .getString("activityId") ?: ""
    }

    private suspend fun setLikedActivity(uid: String, postId: String, title: String) {
        val data = CommunityActivity.like(uid = uid, title = title, targetId = postId).toDto()
        db.collection("activities")
            .add(data)
            .await()
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
                    runCatching { doc.toObject(CommunityActivityDto::class.java)}.getOrNull()
                } ?: emptyList()

                trySend(activities)
            }

        awaitClose { listener.remove() }

    }

    override suspend fun deleteActivities(activityIds: List<String>) {
        activityIds.forEach {
            deleteActivity(activityId = it)
        }
    }

    private suspend fun deleteActivity(uid:String? = null, activityId: String? = null, targetId: String? = null, type : String? = null){

        when(type){
            ActivityType.LIKE -> {
                requireNotNull(uid){"uid 없음"}
                requireNotNull(targetId){"targetId 없음"}

                db.collection("activities")
                    .whereEqualTo("uid", uid)
                    .whereEqualTo("targetId", targetId)
                    .whereEqualTo("type", type)
                    .get()
                    .await()
                    .documents
                    .forEach { doc -> doc.reference.delete().await() }
            }
            null -> {
                requireNotNull(activityId){ " activityId 없음"}

                db.collection("activities")
                    .document(activityId)
                    .delete()
                    .await()
            }
            else -> {
                throw IllegalArgumentException("액티비티 삭제 실패")
            }

        }

    }
}
