package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class PostAuthor(
    val id: String = "",
    val nickname: String = "",
    val profileImg: String = ""
)

// ▼▼▼ 추가: ERD의 comments 서브컬렉션 → user 중첩 맵에 맞추기
data class CommentUser(
    val uid: String = "",
    val nickname: String = "",
    val profileImg: String = ""
)


data class Post(
    val postId : String = "",

    // ▼▼▼ 수정: authorId/authorNickname/authorProfileImg → 중첩 맵
    val author: PostAuthor = PostAuthor(),
    val title: String = "",
    val content: String = "",
    val tag: List<String> = emptyList(),
    val commentCount: Int = 0,
    val likeCount: Int = 0,
    val createdAt: Timestamp = Timestamp.now(),
    val activityId: String = "",
    // ▼▼▼ 추가: UI 전용 필드 — Firestore 직렬화/역직렬화에서 제외
    @get:Exclude
    val isLiked: Boolean = false,               // likes 서브컬렉션에서 조회 후 설정

    @get:Exclude
    val comments: List<Comment> = emptyList()   // comments 서브컬렉션에서 조회 후 설정
)

// ▼▼▼ 수정: ERD 서브컬렉션 구조에 맞춤 — posts/{postId}/comments/{commentId}
data class Comment(
    val commentId: String = "",
    val user: CommentUser = CommentUser(),      // ERD: user { id, nickname, profileImg }
    val content: String = "",
    val activityId: String = "",
    val createdAt: Timestamp? = Timestamp.now()
)

data class PostLike(
    val isLiked: Boolean = false,
    val activityId: String = ""
)


