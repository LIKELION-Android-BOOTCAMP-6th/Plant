package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class ToggleLikeUseCase @Inject constructor(
    private val repository: CommunityRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(postId: String, authorId: String, title: String): Result<Boolean> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }

        // 본인 게시글은 좋아요할 수 없음
        if (user.uid == authorId) return Result.Failure(AppError.Custom("본인 게시글은 좋아요할 수 없습니다."))

        return repository.toggleLike(postId, user.uid, title)
    }
}
