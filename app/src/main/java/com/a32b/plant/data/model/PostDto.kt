package com.a32b.plant.data.model

import com.a32b.plant.domain.model.StudyLog
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
data class PostAuthorDto(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",
    @get:PropertyName("profileImg") @set:PropertyName("profileImg")
    var profileImg: String = ""
)

// ▼▼▼ 추가: ERD의 comments 서브컬렉션 → user 중첩 맵에 맞추기
data class CommentUserDto(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",
    @get:PropertyName("profileImg") @set:PropertyName("profileImg")
    var profileImg: String = ""
)

data class CommentDto(
    @DocumentId
    var commentId: String = "",
    @get:PropertyName("user") @set:PropertyName("user")
    var user: CommentUserDto = CommentUserDto(),      // ERD: user { id, nickname, profileImg }
    @get:PropertyName("content") @set:PropertyName("content")
    var content: String = "",
    @get:PropertyName("activityId") @set:PropertyName("activityId")
    var activityId: String = "",
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp? = Timestamp.now()
)
data class PostDto(
    @DocumentId
    var postId : String = "",
    // ▼▼▼ 수정: authorId/authorNickname/authorProfileImg → 중첩 맵
    @get:PropertyName("author") @set:PropertyName("author")
    var author: PostAuthorDto = PostAuthorDto(),
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("content") @set:PropertyName("content")
    var content: String? = null,
    @get:PropertyName("tag") @set:PropertyName("tag")
    var tag: TagDto = TagDto(),
    @get:PropertyName("commentCount") @set:PropertyName("commentCount")
    var commentCount: Int = 0,
    @get:PropertyName("likeCount") @set:PropertyName("likeCount")
    var likeCount: Int = 0,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp = Timestamp.now(),
    @get:PropertyName("activityId") @set:PropertyName("activityId")
    var activityId: String = "",
    @get:PropertyName("likedBy") @set:PropertyName("likedBy")
    var likedBy: List<String> = emptyList(),
    @get:PropertyName("studyLogs") @set:PropertyName("studyLogs")
    var studyLogs: List<StudyLog>? = null,
    @get:PropertyName("isShared") @set:PropertyName("isShared")
    var isShared: Boolean? = false,
    @get:PropertyName("updatedAt") @set:PropertyName("updatedAt")
    var updatedAt: Timestamp? = null,
)
