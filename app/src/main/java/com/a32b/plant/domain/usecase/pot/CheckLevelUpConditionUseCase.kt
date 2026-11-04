package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.PotLevelConditions
import javax.inject.Inject

class CheckLevelUpConditionUseCase @Inject constructor() {
    operator fun invoke(potLevel: Int, potStudyTimeMs: Long): Boolean {
        val condition = PotLevelConditions.getCondition(potLevel)
            ?: return false // Lv.6 최고 레벨

        val requiredMs = condition.minHoursForLevelUp * 3_600_000L
        return potStudyTimeMs >= requiredMs
    }
}
