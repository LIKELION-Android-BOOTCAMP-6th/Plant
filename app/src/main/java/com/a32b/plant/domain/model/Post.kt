package com.a32b.plant.domain.model


data class PostAuthor(
    val id: String,
    val nickname: String,
    val profileImg: String
)

data class CommentUser(
    val uid: String,
    val nickname: String,
    val profileImg: String
)


data class Post(
    val postId : String,
    val author: PostAuthor,
    val title: String,
    val content: String?,
    val tag: Tag,
    val commentCount: Int,
    val likeCount: Int,
    val bookmarkCount: Int,
    val createdAt: Long?,
    val activityId: String,
    val isLiked: Boolean,
    val isBookmarked: Boolean,
    val studyLogs: List<StudyLog>?,
    var isShared: Boolean?,
    val comments: List<Comment>,
    val updatedAt: Long?
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
            bookmarkCount = 0,
            createdAt = null,
            activityId = "",
            isLiked = false,
            isBookmarked = false,
            studyLogs = null,
            isShared = false,
            comments = emptyList(),
            updatedAt = null
        )

        fun createShared(author: PostAuthor, title: String, studyLogs: List<StudyLog>, tag: Tag, content: String? = null) = Post(
            postId = "",
            author = author,
            title = title,
            content = content,
            tag = tag,
            commentCount = 0,
            likeCount = 0,
            bookmarkCount = 0,
            createdAt = null,
            activityId = "",
            isLiked = false,
            isBookmarked = false,
            studyLogs = studyLogs,
            isShared = true,
            comments = emptyList(),
            updatedAt = null
        )

    }
}

//posts/{postId}/comments/{commentId}
data class Comment(
    val commentId: String,
    val user: CommentUser,      // ERD: user { id, nickname, profileImg }
    val content: String,
    val activityId: String,
    val createdAt: Long?
)

