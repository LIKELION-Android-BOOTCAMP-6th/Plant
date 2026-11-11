package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class GetProfileImageLevelListUseCase @Inject constructor(
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    operator fun invoke(): Result<List<Int>> = ensureCurrentUserUseCase().map { user ->
        (0..user.unlockedProfileLevel.coerceIn(0, PotLevelConditions.MAX_LEVEL)).toList()
    }
}
