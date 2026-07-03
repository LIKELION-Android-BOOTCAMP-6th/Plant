package com.a32b.plant.data.model

import com.a32b.plant.domain.model.CommentUser
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.Tag
import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName
data class PostAuthor(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",
    @get:PropertyName("profileImg") @set:PropertyName("profileImg")
    var profileImg: String = ""
)

// ▼▼▼ 추가: ERD의 comments 서브컬렉션 → user 중첩 맵에 맞추기
data class CommentUser(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",
    @get:PropertyName("profileImg") @set:PropertyName("profileImg")
    var profileImg: String = ""
)

data class Comment(
    @get:PropertyName("commentId") @set:PropertyName("commentId")
    var commentId: String = "",
    @get:PropertyName("user") @set:PropertyName("user")
    var user: CommentUser = CommentUser(),      // ERD: user { id, nickname, profileImg }
    @get:PropertyName("content") @set:PropertyName("content")
    var content: String = "",
    @get:PropertyName("activityId") @set:PropertyName("activityId")
    var activityId: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp? = Timestamp.now()
)
data class PostDto(
    @get:PropertyName("postId") @set:PropertyName("postId")
    var postId : String = "",
    // ▼▼▼ 수정: authorId/authorNickname/authorProfileImg → 중첩 맵
    @get:PropertyName("author") @set:PropertyName("author")
    var author: PostAuthor = PostAuthor(),
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("content") @set:PropertyName("content")
    var content: String? = null,
    @get:PropertyName("tag") @set:PropertyName("tag")
    var tag: Tag = Tag(),
    @get:PropertyName("commentCount") @set:PropertyName("commentCount")
    var commentCount: Int = 0,
    @get:PropertyName("likeCount") @set:PropertyName("likeCount")
    var likeCount: Int = 0,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp = Timestamp.now(),
    @get:PropertyName("activityId") @set:PropertyName("activityId")
    var activityId: String = "",
//    @get:PropertyName("commentId") @set:PropertyName("commentId")
//    var isLiked: Boolean = false, // likes 서브컬렉션에서 조회 후 설정
//    isLiked = currentUid in likedBy 매퍼에서 일케 하기
    @get:PropertyName("studyLogs") @set:PropertyName("studyLogs")
    var studyLogs: List<StudyLog>? = null,
    @get:PropertyName("isShared") @set:PropertyName("isShared")
    var isShared: Boolean? = false,
    @get:PropertyName("comments") @set:PropertyName("comments")
    var comments: List<Comment> = emptyList()   // comments 서브컬렉션에서 조회 후 설정
)
