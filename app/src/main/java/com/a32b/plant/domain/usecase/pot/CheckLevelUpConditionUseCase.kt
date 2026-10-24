package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.item.CanConsumeItemsUseCase
import javax.inject.Inject

class CheckLevelUpConditionUseCase @Inject constructor(
    private val canConsumeItemsUseCase: CanConsumeItemsUseCase
) {
    suspend operator fun invoke(potLevel: Int, potStudyTimeMs: Long): Result<Boolean> {
        val condition = PotLevelConditions.getCondition(potLevel)
            ?: return Result.Success(false) // Lv.6 최고 레벨

        val requiredMs = condition.requiredHours * 3_600_000L
        if (potStudyTimeMs < requiredMs) return Result.Success(false) // 시간 부족

        return canConsumeItemsUseCase(condition.requiredItems) // 아이템 보유 확인
    }
}