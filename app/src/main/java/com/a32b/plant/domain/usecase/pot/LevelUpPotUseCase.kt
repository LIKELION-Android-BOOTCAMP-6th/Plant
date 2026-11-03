package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class LevelUpPotUseCase @Inject constructor(
    private val potRepository: PotRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(potId: String, currentPotLevel: Int): Result<Unit> {
        val condition = PotLevelConditions.getCondition(currentPotLevel)
            ?: return Result.Failure(AppError.Custom("이미 최고 레벨입니다."))

        val user = when (val r = ensureCurrentUserUseCase()) {
            is Result.Success -> r.data
            is Result.Failure -> return Result.Failure(r.error)
        }

        return potRepository.levelUpWithItemConsumption(
            uid = user.uid,
            potId = potId,
            newLevel = currentPotLevel + 1,
            items = condition.requiredItems
        )
    }
}