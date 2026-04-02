package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.core.util.ActivityType
import com.a32b.plant.data.di.AppContainer.firestore
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.Comment
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.model.CommunityActivity
import com.a32b.plant.data.model.Tag
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.toObjects
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.text.get

class PostRepository(private val db: FirebaseFirestore) {

    //글 목록 조회
    fun getPostList(): Flow<List<Post>> = callbackFlow {
        val subscription = db.collection("posts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        val likedBy = doc.get("likedBy") as? List<*> ?: emptyList<String>()
                        doc.toObject(Post::class.java)?.copy(postId = doc.id, isLiked = CurrentUser.uid in likedBy)
                    } catch (e: Exception) {
                        Log.e("파싱오류", "문서 ID: ${doc.id}, 데이터: ${doc.data}")
                        null
                    }
                }?.sortedByDescending { it.createdAt } ?: emptyList()

                trySend(posts)
            }
        awaitClose { subscription.remove() }
    }

    //상세 조회
    fun getPostDetail(postId: String): Flow<Post?> = callbackFlow {
        val subscription = db.collection("posts").document(postId)
            .addSnapshotListener { snapshot, _ ->
                val likedBy = snapshot?.get("likedBy") as? List<*> ?: emptyList<String>()
                val post = snapshot?.toObject(Post::class.java)?.copy(postId = snapshot.id, isLiked = CurrentUser.uid in likedBy)
                trySend(post)
            }
        awaitClose { subscription.remove() }
    }

    //게시글 저장
    suspend fun savePost(post: Post, activity: CommunityActivity): String{
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


    // 댓글 수정 하려면 commentId 필요! 아래의 원래 함수는 comments 컬렉션의 {commentsId} 문서 안의 필드를 전부 가져오나,
    // 댓글 수정 및 삭제를 하려면 문서 {commentsId} 자체가 필요하고 firestore에서 문서 ID는 필드가 아니라 경로라서
    // 아래와 같이 각 문서를 순회하면서 doc.id로 문서 ID를 직접 꺼내서 copy해야함.
    suspend fun getComments(postId: String): List<Comment> {
        return db.collection("posts").document(postId)
            .collection("comments")
            .get()
            .await()
            .documents.mapNotNull { doc ->
                doc.toObject(Comment::class.java)?.copy(commentId = doc.id)
            }
            // 정렬 방식 : 최신이 맨 아래로 가게
            .sortedBy { it.createdAt }
    }


    // 댓글 남기기 함수
    suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity) {

        val commentRef = db.collection("posts").document(postId)
                            .collection("comments").document()
        val activityRef = db.collection("activities").document()

        val commentWithAct = comment.copy(activityId = activityRef.id)
        val activityWithCo = activity.copy(targetId = postId, commentId = commentRef.id)

        db.runBatch { batch ->
            batch.set(commentRef, commentWithAct)
            batch.set(activityRef, activityWithCo)
        }.await()

        db.collection("posts").document(postId)
            .update("commentCount", FieldValue.increment(1))

    }

    // 댓글 수정 함수
    suspend fun updateComment(postId: String, commentId: String, newContent: String) {
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

    // 댓글 삭제 함수 (activity도 함께 삭제 + commentCount 감소)
    suspend fun deleteComment(postId: String, commentId: String) {
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
    suspend fun getActivityId(postId: String) : String{
        return db.collection("posts").document(postId)
            .get()
            .await()
            .getString("activityId")!!
    }

    suspend fun deletePost(postId: String) {
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

    suspend fun updatePost(isShared: Boolean, postId: String, title: String, content: String? = null, tag: List<String>? = null, createdAt: Timestamp? = null) {
        if(isShared){
            db.collection("posts").document(postId)
                .update("title", title)
                .await()
        }else{
            db.collection("posts").document(postId)
                .update(
                    "title", title,
                    "content", content,
                    "tag", tag,
                    "createdAt", createdAt
                )
                .await()
        }

        db.collection("activities").document(getActivityId(postId))
            .update("title", title)
            .await()
    }

    suspend fun toggleLike(postId: String, uid: String, isAlreadyLiked: Boolean, title: String) {
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
            setLikedActivity(postId, title)
        }
    }

    suspend fun setLikedActivity(postId: String, title: String){
        val data = CommunityActivity(type = ActivityType.LIKE, title = title, targetId = postId)
        db.collection("activities")
            .add(data)
            .await()
    }
    suspend fun deleteLikedActivity(postId: String){
        val docId = db.collection("activities")
            .whereEqualTo("targetId", postId)
            .whereEqualTo("type", ActivityType.LIKE)
            .get()
            .await()
            .documents
            .firstOrNull()?.reference?.id
        docId?.let {
            db.collection("activities").document(docId)
                .delete()
                .await()
        }

    }
//    suspend fun uploadPostAndReturnId(post: Post): String {
//        return db.collection("posts").add(post).await().id
//    }


    suspend fun getTag():List<Tag>{
        return try {
            db.collection("Tags")
                .get()
                .await()
                .toObjects(Tag::class.java)
        } catch (e: Exception){
            emptyList()
        }

    }
}