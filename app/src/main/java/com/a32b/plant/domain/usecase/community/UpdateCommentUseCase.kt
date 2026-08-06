package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class UpdateCommentUseCase @Inject constructor(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(postId: String, commentId: String, newContent: String): Result<Unit> =
        repository.updateComment(postId, commentId, newContent)
}
