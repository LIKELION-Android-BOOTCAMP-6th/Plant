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
){
    companion object{
        fun createOriginal(author: PostAuthor, title: String, content: String, tag: Tag) = Post(
            postId = "",
            author = author,
            title = title,
            content = content,
            tag = tag,
            commentCount = 0,
            likeCount = 0,
            createdAt = null,
            activityId = "",
            isLiked = false,
            studyLogs = null,
            isShared = false
        )

        fun createShared(author: PostAuthor, title: String, studyLogs: List<StudyLog>, tag: Tag) = Post(
            postId = "",
            author = author,
            title = title,
            content = null,
            tag = tag,
            commentCount = 0,
            likeCount = 0,
            createdAt = null,
            activityId = "",
            isLiked = false,
            studyLogs = studyLogs,
            isShared = true
        )

    }
}

//posts/{postId}/comments/{commentId}
data class Comment(
    val commentId: String,
    val user: CommentUser,      // ERD: user { id, nickname, profileImg }
    val content: String,
    val activityId: String,
    val createdAt: Long?,
    // 아직 서버에 반영되지 않고 로컬에만 있는(오프라인 등) 상태인지
    val isPending: Boolean = false
)

