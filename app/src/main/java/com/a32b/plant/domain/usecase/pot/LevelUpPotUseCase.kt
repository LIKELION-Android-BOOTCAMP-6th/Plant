package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

/**
 * 화분 레벨업 (#456).
 *
 * 조건은 기기 캐시 값으로 확인한다 (오프라인에서도 레벨업할 수 있게).
 * - 화분: 캐시의 화분 문서 (아직 서버에 가지 않은 학습 저장도 반영됨)
 * - 아이템: 현재 유저 (users 문서 실시간 구독 값 — 캐시에 반영된 쓰기 포함)
 * 조건을 채우지 못하면 요청하지 않고 부족한 항목을 알려준다.
 * 서버에서는 보안 규칙이 같은 조건 표로 다시 검증하므로, 캐시 값으로는 충족했어도 서버 값으로 부족하면(다른 기기에서 아이템 사용 등) 거부된다.
 */
class LevelUpPotUseCase @Inject constructor(
    private val potRepository: PotRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(potId: String): Result<SaveResult> {
        val user = when (val r = ensureCurrentUserUseCase()) {
            is Result.Success -> r.data
            is Result.Failure -> return Result.Failure(r.error)
        }

        val pot = when (val r = potRepository.getPotPreferringCache(user.uid, potId)) {
            is Result.Success -> r.data ?: return Result.Failure(AppError.Custom(POT_NOT_FOUND_MESSAGE))
            is Result.Failure -> return Result.Failure(r.error)
        }
        if (pot.isCompleted) return Result.Failure(AppError.Custom(COMPLETED_POT_MESSAGE))

        val condition = PotLevelConditions.getCondition(pot.potLevel)
            ?: return Result.Failure(AppError.Custom(MAX_LEVEL_MESSAGE))

        val requiredMs = condition.minHoursForLevelUp * HOUR_MS
        if (pot.potTotalStudyingTime < requiredMs) {
            // 남은 시간이 1초 미만이어도 "0초"가 되지 않게 초 단위로 올림
            val remainingMs = (requiredMs - pot.potTotalStudyingTime + 999) / 1000 * 1000
            return Result.Failure(
                AppError.Custom("레벨업까지 학습 시간이 ${TimeFormatter.formatToKoreanDuration(remainingMs)} 남았어요.")
            )
        }

        val lackingItems = condition.requiredItems.filter { (type, count) -> user.item.countOf(type) < count }.keys
        if (lackingItems.isNotEmpty()) {
            return Result.Failure(
                AppError.InsufficientItems("${lackingItems.joinToString { it.kor }}이(가) 부족합니다.")
            )
        }

        return potRepository.levelUpPot(
            uid = user.uid,
            potId = potId,
            currentLevel = pot.potLevel,
            items = condition.requiredItems
        )
    }

    companion object {
        private const val HOUR_MS = 60 * 60 * 1000L
        const val POT_NOT_FOUND_MESSAGE = "존재하지 않거나 삭제된 화분입니다."
        const val COMPLETED_POT_MESSAGE = "완료된 화분은 레벨업할 수 없습니다."
        const val MAX_LEVEL_MESSAGE = "이미 최고 레벨입니다."
    }
}
