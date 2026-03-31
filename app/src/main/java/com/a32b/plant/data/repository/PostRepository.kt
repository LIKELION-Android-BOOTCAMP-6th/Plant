package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.di.AppContainer.firestore
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.Post
import com.a32b.plant.data.model.CommunityActivity
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val db: FirebaseFirestore
) {

    //글 목록 조회
    fun getPosts(): Flow<List<Post>> = callbackFlow {
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

    //상세 조회??
    fun getPost(postId: String): Flow<Post?> = callbackFlow {
        val subscription = db.collection("posts").document(postId)
            .addSnapshotListener { snapshot, _ ->
                val post = snapshot?.toObject(Post::class.java)?.copy(postId = snapshot.id)
                trySend(post)
            }
        awaitClose { subscription.remove() }
    }

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

    suspend fun addComment(postId: String, uid: String, nickName: String, content: String) {
        val newComment = hashMapOf(
            "uid" to uid,
            "nickName" to nickName,
            "content" to content,
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("posts").document(postId)
            .update(
                "comments", FieldValue.arrayUnion(newComment),
                "commentCount", FieldValue.increment(1)
            )
            .await()
    }

    suspend fun deletePost(postId: String) {
        db.collection("posts").document(postId).delete().await()
    }

    suspend fun uploadPost(post: Post) {
        db.collection("posts").add(post).await()
    }

    suspend fun updatePost(postId: String, title: String, content: String, tag: List<String>) {
        db.collection("posts").document(postId)
            .update(
                "title", title,
                "content", content,
                "tag", tag
            )
            .await()
    }
    suspend fun addCommunityActivity(activity: CommunityActivity){
        //댓글, 좋아요 시 커뮤니티 활동 추가할 것
        val data = hashMapOf(
            "uid" to CurrentUser.uid,
            "type" to activity.type,
            "title" to activity.title,
            "targetId" to activity.targetId,
            "createdAt" to activity.createAt
        )

        activity.comment?.let { data["comment"] = it }

        db.collection("activities")
            .add(data)
            .await()

    }
    fun getLiked(): Boolean{
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