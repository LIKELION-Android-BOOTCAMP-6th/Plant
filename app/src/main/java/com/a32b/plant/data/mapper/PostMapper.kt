package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.data.model.CommentDto
import com.a32b.plant.data.model.CommentUserDto
import com.a32b.plant.data.model.PostAuthorDto
import com.a32b.plant.data.model.PostDto
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommentUser
import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.model.PostAuthor


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
    createdAt = createdAt.toLong()
)
fun PostDto.toDomain(uid: String, comments: List<Comment>) : Post = Post(
    postId = postId,
    author = author.toDomain(),
    title = title,
    content = content,
    tag = tag.toDomain(),
    commentCount = commentCount,
    likeCount = likeCount,
    createdAt = createdAt.toLong(),
    activityId = activityId,
    isLiked = uid in likedBy,
    studyLogs = studyLogs,
    isShared = isShared,
    comments = comments
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
    studyLogs = studyLogs,
    isShared = isShared
)