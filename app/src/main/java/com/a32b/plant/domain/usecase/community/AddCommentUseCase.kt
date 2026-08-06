package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommentUser
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class AddCommentUseCase @Inject constructor(
    private val repository: CommunityRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {

    suspend operator fun invoke(postId: String, title: String, content: String): Result<Unit> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }

        return repository.addComment(
            postId = postId,
            comment = Comment(
                commentId = "",
                user = CommentUser(user.uid, user.nickname, user.profileImg),
                content = content,
                activityId = "",
                createdAt = null
            ),
            activity = CommunityActivity.comment(
                uid = user.uid, title = title, targetId = postId, content = content, commentId = ""
            )
        )
    }
}
