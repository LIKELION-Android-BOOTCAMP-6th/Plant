package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.repository.BlockRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

/** 차단 해제 (#660). 해제한 사용자의 글은 목록을 다시 불러오지 않아도 다음 화면 갱신부터 다시 보인다 */
class UnblockUserUseCase @Inject constructor(
    private val repository: BlockRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(blockedUid: String): Result<SaveResult> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }
        return repository.unblockUser(user.uid, blockedUid)
    }
}
