package com.a32b.plant.domain.usecase.item

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.ItemRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

/**
 * 보너스 박스 열기 (#454). 추첨·지급은 서버 함수가 하고, 앱은 뽑힌 목록만 받는다 (온라인 전용).
 *
 * @param requestId 같은 요청을 다시 보낼 때(응답을 받지 못한 뒤 재시도) 같은 값을 넘긴다.
 *   서버가 이미 처리한 요청이면 다시 추첨하지 않고 이전 결과를 돌려준다.
 */
class OpenBonusBoxUseCase @Inject constructor(
    private val repository: ItemRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(count: Int, requestId: String): Result<List<ItemType>> {
        if (count !in 1..MAX_OPEN_COUNT) return Result.Failure(AppError.Debug("박스 count 오류: $count"))

        // 사용자는 서버가 로그인 토큰으로 확인한다. 여기서는 세션 만료만 먼저 확인한다
        val sessionCheck = ensureCurrentUserUseCase()
        if (sessionCheck is Result.Failure) return Result.Failure(sessionCheck.error)

        return repository.openBonusBoxes(count, requestId)
    }

    companion object {
        /** 한 번에 열 수 있는 최대 개수. 서버 BONUS_BOX_MAX_OPEN_COUNT(functions/index.js)와 같아야 한다 */
        const val MAX_OPEN_COUNT = 100
    }
}
