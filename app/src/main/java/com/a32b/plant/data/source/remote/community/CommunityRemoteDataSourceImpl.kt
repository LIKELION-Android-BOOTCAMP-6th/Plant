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
import com.google.firebase.firestore.MetadataChanges
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

    override fun getPostDetail(postId: String): Flow<PostDto?> = callbackFlow {
        val subscription = db.collection("posts").document(postId)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }
                trySend(snapshot?.toObject(PostDto::class.java))
            }
        awaitClose { subscription.remove() }
    }

    override fun observeComments(postId: String): Flow<List<CommentDto>> = callbackFlow {
        val subscription = db.collection("posts").document(postId)
            .collection("comments")
            // 정렬 방식 : 최신이 맨 아래로 가게
            .orderBy("createdAt", Query.Direction.ASCENDING)
            // 문서 내용이 아닌 hasPendingWrites 같은 메타데이터만 바뀌어도(서버 ack 등) 다시 알림받기 위함
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val comments = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(CommentDto::class.java)?.apply {
                        // 아직 서버 ack를 못 받고 로컬 캐시에만 있는 쓰기인지(오프라인 등)
                        isPending = doc.metadata.hasPendingWrites()
                    }
                } ?: emptyList()

                trySend(comments)
            }
        awaitClose { subscription.remove() }
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
                .update("title", title)
                .await()
        } else {
            db.collection("posts").document(postId)
                .update(
                    "title", title,
                    "content", content,
                    "tag", tag,
                    "createdAt", Timestamp.now()
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

        // 2. 관련 activity 한번에 삭제 (게시글 + 댓글 activity 모두)
        db.collection("activities")
            .whereEqualTo("targetId", postId)
            .get().await()
            .documents
            .forEach { doc -> doc.reference.delete().await() }

        // 3. 게시글 문서 삭제
        postRef.delete().await()
    }

    override suspend fun toggleLike(postId: String, uid: String, isAlreadyLiked: Boolean, title: String) {
        val postRef = db.collection("posts").document(postId)
        if (isAlreadyLiked) {
            postRef.update(
                "likedBy", FieldValue.arrayRemove(uid),
                "likeCount", FieldValue.increment(-1)
            ).await()
            deleteLikedActivity(postId)
        } else {
            postRef.update(
                "likedBy", FieldValue.arrayUnion(uid),
                "likeCount", FieldValue.increment(1)
            ).await()
            setLikedActivity(uid, postId, title)
        }
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

    private suspend fun deleteLikedActivity(postId: String) {
        val docId = db.collection("activities")
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
