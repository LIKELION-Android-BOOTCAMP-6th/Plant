package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.data.model.CommentDto
import com.a32b.plant.data.model.CommentUserDto
import com.a32b.plant.data.model.PostAuthorDto
import com.a32b.plant.data.model.PostDto
import com.a32b.plant.data.model.StudyLogEmbedDto
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommentUser
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostAuthor
import com.a32b.plant.domain.model.StudyLog


/**
 Dto -> toDomain
 */
fun PostAuthorDto.toDomain() : PostAuthor = PostAuthor(
    id, nickname, profileImg
)
fun CommentUserDto.toDomain() : CommentUser = CommentUser(
    uid, nickname, profileImg
)
fun CommentDto.toDomain() : Comment = Comment(
    commentId = commentId,
    user = user.toDomain(),
    content = content,
    activityId = activityId,
    createdAt = createdAt.toLong(),
    reportCount = reportCount,
    isHidden = isHidden,
    isPending = false
)
fun StudyLogEmbedDto.toDomain(): StudyLog = StudyLog(
    title = title,
    contents = contents,
    // 길이가 모자라면 미완료(false)로 채우고, 길어도 contents 길이에 맞춰 자른다 (기존 공유글 하위 호환)
    completedContents = List(contents.size) { index -> completedContents.getOrNull(index) ?: false },
    studyingTime = studyingTime,
    createAt = createAt,
    startedAt = startedAt,
    id = id,
    isSelected = isSelected
)

fun StudyLog.toEmbedDto(): StudyLogEmbedDto = StudyLogEmbedDto(
    title = title,
    contents = contents,
    completedContents = completedContents,
    studyingTime = studyingTime,
    createAt = createAt,
    startedAt = startedAt,
    id = id,
    isSelected = isSelected
)

fun PostDto.toDomain(uid: String, comments: List<Comment>) : Post = Post(
    postId = postId,
    author = author.toDomain(),
    title = title,
    content = content,
    tag = tag.toDomain(),
    commentCount = commentCount,
    likeCount = likeCount,
    bookmarkCount = bookmarkCount,
    createdAt = createdAt.toLong(),
    activityId = activityId,
    isLiked = uid in likedBy,
    isBookmarked = uid in bookmarkedBy,
    studyLogs = studyLogs?.map { it.toDomain() },
    isShared = isShared,
    comments = comments,
    updatedAt = updatedAt?.toDate()?.time,
    reportCount = reportCount,
    isHidden = isHidden
)

/**
 Domain -> Dto
 */

fun PostAuthor.toDto() : PostAuthorDto = PostAuthorDto(
    id, nickname, profileImg
)

fun CommentUser.toDto() : CommentUserDto = CommentUserDto(
    uid, nickname, profileImg
)

fun Comment.toDto() : CommentDto = CommentDto(
    commentId = commentId,
    user = user.toDto(),
    content = content,
    activityId = activityId,
    createdAt = createdAt.toTimestamp()
)

fun Post.toDto() : PostDto = PostDto(
    postId = postId,
    author = author.toDto(),
    title = title,
    content = content,
    tag = tag.toDto(),
    commentCount = commentCount,
    likeCount = likeCount,
    createdAt = createdAt.toTimestamp(),
    activityId = activityId,
    studyLogs = studyLogs?.map { it.toEmbedDto() },
    isShared = isShared,
    updatedAt = updatedAt?.toTimestamp()
)
