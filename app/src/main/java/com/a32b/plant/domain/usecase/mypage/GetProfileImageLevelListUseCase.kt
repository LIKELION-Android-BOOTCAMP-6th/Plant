package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

private const val MAX_PROFILE_LEVEL = 6 // 프로필 이미지 최고 화분 레벨

class GetProfileImageLevelListUseCase @Inject constructor(
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    operator fun invoke(): Result<List<Int>> = ensureCurrentUserUseCase().map { user ->
        (0..user.unlockedProfileLevel.coerceIn(0, MAX_PROFILE_LEVEL)).toList()
    }
}
