package com.a32b.plant.domain.model

import java.util.UUID

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

// 게시글·댓글 자동 블라인드 기준 신고 횟수 (#308) — Post.isBlind / Comment.isBlind 공통
private const val BLIND_REPORT_THRESHOLD = 5

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
    val updatedAt: Long?,
    val reportCount: Int,
    val isHidden: Boolean
){
    // 숨김 처리됐거나 신고가 기준 횟수 이상 누적된 게시글 — 화면에서 제목/본문 대신 블라인드 문구를 표시하고 검색에서도 제외
    val isBlind: Boolean get() = isHidden || reportCount >= BLIND_REPORT_THRESHOLD

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
            updatedAt = null,
            reportCount = 0,
            isHidden = false
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
            updatedAt = null,
            reportCount = 0,
            isHidden = false
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
    val reportCount: Int,
    val isHidden: Boolean,
    // 서버 반영 전 화면에만 표시하는 임시 댓글 (낙관적 UI) — Firestore에는 저장되지 않는 값
    val isPending: Boolean
) {
    // 숨김 처리됐거나 신고가 기준 횟수 이상 누적된 댓글 — 화면에서 내용 대신 블라인드 문구를 표시
    val isBlind: Boolean get() = isHidden || reportCount >= BLIND_REPORT_THRESHOLD

    companion object {
        // commentId는 Firestore ID가 아니라 임시 댓글끼리 구분하기 위한 로컬 키 (서버 반영 전까지 수정/삭제/신고 불가)
        fun createPending(user: CommentUser, content: String, createdAt: Long) = Comment(
            commentId = "pending_${UUID.randomUUID()}",
            user = user,
            content = content,
            activityId = "",
            createdAt = createdAt,
            reportCount = 0,
            isHidden = false,
            isPending = true
        )
    }
}

