package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.domain.repository.UserRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class ObserveDarkModeUseCase @Inject constructor(
    private val userRepository: UserRepository
) {
    // 기기의 다크모드 설정을 구독하고, 읽기 실패로 구독이 끝나면 잠시 후 다시 구독한다.
    operator fun invoke(): Flow<Boolean> = flow {
        while (true) {
            emitAll(userRepository.observeDarkMode())
            delay(RETRY_DELAY_MILLIS)
        }
    }

    private companion object {
        const val RETRY_DELAY_MILLIS = 5_000L
    }
}
