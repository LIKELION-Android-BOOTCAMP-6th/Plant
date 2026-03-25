package com.a32b.plant.data.repository

import com.a32b.plant.ui.feature.community.ui.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class PostRepository(private val db: FirebaseFirestore) {

    // ✅ ViewModel이 호출하는 바로 그 함수! (실시간으로 게시글 리스트를 가져옴)
    fun getPosts(): Flow<List<Post>> = callbackFlow {
        // "posts"라는 이름의 파이어베이스 컬렉션(폴더)을 감시합니다.
        val subscription = db.collection("posts")
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    // 에러가 나면 빈 리스트를 보냄
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                // 서버에서 가져온 데이터를 Post라는 우리가 만든 규격으로 변환
                val posts = snapshot?.documents?.mapNotNull { doc ->
                    doc.toObject(Post::class.java)?.copy(id = doc.id)
                } ?: emptyList()

                trySend(posts) // 데이터를 ViewModel로 전달!
            }

        // 앱이 꺼지거나 화면을 나가면 감시를 중단함 (배터리 절약)
        awaitClose { subscription.remove() }
    }

    // ✅ 지난번에 만든 글쓰기 저장 함수 (함께 있으면 좋습니다)
    suspend fun uploadPost(post: Post) {
        db.collection("posts").add(post)
    }
}