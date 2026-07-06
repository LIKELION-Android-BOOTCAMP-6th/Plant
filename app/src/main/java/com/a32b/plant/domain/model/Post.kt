package com.a32b.plant.domain.model


data class PostAuthor(
    val id: String,
    val nickname: String,
    val profileImg: String
)

// ▼▼▼ 추가: ERD의 comments 서브컬렉션 → user 중첩 맵에 맞추기
data class CommentUser(
    val uid: String = "",
    val nickname: String = "",
    val profileImg: String = ""
)


data class Post(
    val postId : String,
    // ▼▼▼ 수정: authorId/authorNickname/authorProfileImg → 중첩 맵
    val author: PostAuthor,
    val title: String,
    val content: String?,
    val tag: Tag,
    val commentCount: Int,
    val likeCount: Int,
    val createdAt: Long?,
    val activityId: String,
    val isLiked: Boolean, // likes 서브컬렉션에서 조회 후 설정
    val studyLogs: List<StudyLog>?,
    var isShared: Boolean?,
    val comments: List<Comment> = emptyList()   // comments 서브컬렉션에서 조회 후 설정
)

//posts/{postId}/comments/{commentId}
data class Comment(
    val commentId: String,
    val user: CommentUser,      // ERD: user { id, nickname, profileImg }
    val content: String,
    val activityId: String,
    val createdAt: Long?
)

