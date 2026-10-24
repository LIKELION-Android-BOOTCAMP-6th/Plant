package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.item.ConsumeItemsUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class LevelUpPotUseCase @Inject constructor(
    private val potRepository: PotRepository,
    private val consumeItemsUseCase: ConsumeItemsUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(potId: String, currentPotLevel: Int): Result<Unit> {
        val condition = PotLevelConditions.getCondition(currentPotLevel)
            ?: return Result.Failure(AppError.Custom("이미 최고 레벨입니다."))

        val user = when (val r = ensureCurrentUserUseCase()) {
            is Result.Success -> r.data
            is Result.Failure -> return Result.Failure(r.error)
        }

        // 1단계: 아이템 차감 — 잔량 부족이면 Failure 반환, 화분 레벨은 변경되지 않음
        val consumeResult = consumeItemsUseCase(condition.requiredItems)
        if (consumeResult is Result.Failure) return consumeResult

        // 2단계: Firestore potLevel 업데이트
        val newLevel = currentPotLevel + 1
        return potRepository.updatePotLevel(
            uid = user.uid,
            potId = potId,
            newLevel = newLevel
        )
    }
}