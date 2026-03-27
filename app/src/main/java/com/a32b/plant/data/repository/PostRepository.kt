package com.a32b.plant.data.repository

import com.a32b.plant.data.model.Post
import com.a32b.plant.data.model.Author
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class PostRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) {

    fun getPosts(): Flow<List<Post>> = callbackFlow {
        val subscription = db.collection("posts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    close(error)
                    return@addSnapshotListener
                }

                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(posts)
            }
        awaitClose { subscription.remove() }
    }

    fun getCurrentUser(): Flow<Author?> = callbackFlow {
        val uid = auth.currentUser?.uid
        if (uid == null) {
            trySend(null)
            close()
            return@callbackFlow
        }
        val subscription = db.collection("users").document(uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.toObject(Author::class.java))
            }
        awaitClose { subscription.remove() }
    }

    fun getPost(postId: String): Flow<Post?> = callbackFlow {
        val subscription = db.collection("posts").document(postId)
            .addSnapshotListener { snapshot, _ ->
                val post = snapshot?.toObject(Post::class.java)?.copy(id = snapshot.id)
                trySend(post)
            }
        awaitClose { subscription.remove() }
    }

    suspend fun addComment(postId: String, nickName: String, content: String) {
        val newComment = hashMapOf(
            "nickName" to nickName,
            "content" to content,
            "createdAt" to System.currentTimeMillis()
        )

        // 댓글 추가 및 댓글 수(commentCount) 1 증가
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
}
