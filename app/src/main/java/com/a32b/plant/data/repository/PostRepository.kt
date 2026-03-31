package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.di.AppContainer.firestore
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.Comment
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.model.CommunityActivity
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
                        doc.toObject(Post::class.java)?.copy(postId = doc.id)
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
                val post = snapshot?.toObject(Post::class.java)?.copy(postId = snapshot.id)
                trySend(post)
            }
        awaitClose { subscription.remove() }
    }

    //게시글 저장
    suspend fun savePost(post: Post, activity: CommunityActivity){
        val postRef = db.collection("posts").document()
        val activityRef = db.collection("activities").document()

        val postWithAct = post.copy(activityId = activityRef.id)
        val activityWithPost = activity.copy(targetId = postRef.id)

        db.runBatch { batch ->
            batch.set(postRef, postWithAct)
            batch.set(activityRef, activityWithPost)
        }.await()
    }

    suspend fun getComments(postId: String): List<Comment>{
       return  db.collection("posts").document(postId)
            .collection("comments")
            .get()
            .await()
            .toObjects<Comment>()
            .sortedByDescending { it.createdAt }

    }

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

    suspend fun getActivityId(postId: String) : String{
        return db.collection("posts").document(postId)
            .get()
            .await()
            .getString("activityId")!!
    }

    suspend fun deletePost(postId: String) {
        val activityId = getActivityId(postId)
        db.collection("posts").document(postId).delete().await()
        db.collection("activities").document(activityId).delete().await()
    }

    suspend fun updatePost(postId: String, title: String, content: String, tag: List<String>) {
        db.collection("posts").document(postId)
            .update(
                "title", title,
                "content", content,
                "tag", tag
            )
            .await()
        db.collection("activities").document(getActivityId(postId))
            .update("title", title)
            .await()
    }

    fun getLiked(): Boolean{
        db.collection("post")
        //post/{postId}/liked/{Current.uid}
        // isLiked 여부를 리턴하는 걸로
        return false
    }

    suspend fun toggleLike(postId: String, uid: String, isAlreadyLiked: Boolean) {
        val postRef = db.collection("posts").document(postId)
        if (isAlreadyLiked) {
            postRef.update(
                "likedBy", FieldValue.arrayRemove(uid),
                "likeCount", FieldValue.increment(-1)
            ).await()
        } else {
            postRef.update(
                "likedBy", FieldValue.arrayUnion(uid),
                "likeCount", FieldValue.increment(1)
            ).await()
        }
    }
    suspend fun uploadPostAndReturnId(post: Post): String {
        return db.collection("posts").add(post).await().id
    }
}