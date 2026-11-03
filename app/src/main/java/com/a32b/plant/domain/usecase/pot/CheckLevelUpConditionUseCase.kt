package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class CheckLevelUpConditionUseCase @Inject constructor() {
    operator fun invoke(potLevel: Int, potStudyTimeMs: Long): Result<Boolean> {
        val condition = PotLevelConditions.getCondition(potLevel)
            ?: return Result.Success(false) // Lv.6 최고 레벨

        val requiredMs = condition.minHoursForLevelUp * 3_600_000L
        val isTimeSatisfied = potStudyTimeMs >= requiredMs

        return Result.Success(isTimeSatisfied) // 아이템 보유 확인
    }
}
