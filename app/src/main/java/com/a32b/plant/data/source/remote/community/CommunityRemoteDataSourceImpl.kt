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

    override suspend fun deletePost(postId: String) {
        val postRef = db.collection("posts").document(postId)

        // 1. 하위 컬렉션 comments 문서 삭제
        val comments = postRef.collection("comments").get().await()
        for (commentDoc in comments.documents) {
            commentDoc.reference.delete().await()
        }

        // 2. 게시글 자신의 activity만 삭제 (댓글/좋아요는 남의 활동 기록이라 건드리지 않음)
        db.collection("activities")
            .whereEqualTo("targetId", postId)
            .whereEqualTo("type", ActivityType.POST)
            .get().await()
            .documents
            .forEach { doc -> doc.reference.delete().await() }

        // 3. 게시글 문서 삭제
        postRef.delete().await()
    }

    override suspend fun toggleLike(postId: String, uid: String, isAlreadyLiked: Boolean, title: String) {
        val postRef = db.collection("posts").document(postId)

        // 트랜잭션으로 likeCount를 원자적으로 read-modify-write → DB에 음수 저장 방지
        db.runTransaction { txn ->
            val current = txn.get(postRef).getLong("likeCount") ?: 0L
            if (isAlreadyLiked) {
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
        }.await()

        if (isAlreadyLiked) deleteLikedActivity(uid, postId)
        else setLikedActivity(uid, postId, title)
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
            db.collection("activities").document(activityId).delete().await()
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

    private suspend fun deleteLikedActivity(uid: String, postId: String) {
        val docId = db.collection("activities")
            .whereEqualTo("uid", uid)           // 본인 활동 기록만 조회 (보안 규칙 통과)
            .whereEqualTo("targetId", postId)
            .whereEqualTo("type", ActivityType.LIKE)
            .get()
            .await()
            .documents
            .firstOrNull()?.reference?.id
        docId?.let {
            db.collection("activities").document(it)
                .delete()
                .await()
        }
    }
}
