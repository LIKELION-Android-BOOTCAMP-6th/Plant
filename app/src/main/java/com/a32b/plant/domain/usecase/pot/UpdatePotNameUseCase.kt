package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.util.PotNameRule
import javax.inject.Inject

class UpdatePotNameUseCase @Inject constructor(
    private val potRepository: PotRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    /**
     * 화분 이름 변경 (#563).
     * - 유저 없음: 세션 만료 처리 후 `AppError.UnknownUser` 실패 (호출부는 토스트를 띄우지 않는다)
     * - 앞뒤 공백을 지운 값([PotNameRule.normalize])으로 저장한다 (화분 생성과 같은 규칙)
     * - 지운 값이 [currentName]과 같으면 저장하지 않고 [SaveResult.Saved] (불필요한 대기열 쓰기 방지)
     *
     * 빈 값(공백만 입력)은 화면에서 확인 버튼을 막으므로 여기까지 오지 않는다.
     * 그래도 들어오면 빈 이름이 저장되지 않도록 저장하지 않고 [SaveResult.Saved]를 돌려준다.
     */
    suspend operator fun invoke(
        potId: String,
        currentName: String,
        newName: String
    ): Result<SaveResult> {
        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }

        val name = PotNameRule.normalize(newName)
        if (name.isEmpty() || name == currentName) return Result.Success(SaveResult.Saved)

        return potRepository.updatePotName(user.uid, potId, name)
    }
}
