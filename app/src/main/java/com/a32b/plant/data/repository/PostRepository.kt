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
                val author = snapshot?.toObject(Author::class.java)?.copy(uid = uid)
                trySend(author)
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

    suspend fun updatePost(postId: String, title: String, content: String, tag: String) {
        db.collection("posts").document(postId)
            .update(
                "title", title,
                "content", content,
                "tag", tag
            )
            .await()
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
}