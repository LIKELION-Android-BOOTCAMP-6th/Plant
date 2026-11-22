package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.BlockedUser
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.repository.BlockRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

/** 게시글·댓글 작성자 차단 (#660). 차단한 사용자의 글·댓글은 커뮤니티에서 내 화면에만 숨겨진다 */
class BlockUserUseCase @Inject constructor(
    private val repository: BlockRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(uid: String, nickname: String, profileImg: String): Result<SaveResult> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        // 화면은 내 글·내 댓글에 차단 메뉴를 보여 주지 않지만, 규칙 거부까지 가지 않도록 여기서도 막는다
        if (uid.isBlank()) return Result.Failure(AppError.Custom(INVALID_TARGET_MESSAGE))
        if (uid == user.uid) return Result.Failure(AppError.Custom(SELF_BLOCK_MESSAGE))

        return repository.blockUser(user.uid, BlockedUser.create(uid, nickname, profileImg))
    }

    companion object {
        const val INVALID_TARGET_MESSAGE = "차단할 사용자를 찾을 수 없어요."
        const val SELF_BLOCK_MESSAGE = "나 자신은 차단할 수 없어요."
    }
}
