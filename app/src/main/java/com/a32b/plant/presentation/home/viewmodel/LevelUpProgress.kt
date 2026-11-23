package com.a32b.plant.presentation.home.viewmodel

import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.PotLevelConditions

/** 홈 화분 카드의 레벨업 진행 바 값(0~1)과 문구 */
internal data class LevelUpProgress(
    val progress: Float,
    val text: String
)

internal const val MAX_LEVEL_PROGRESS_TEXT = "최고 레벨"

/**
 * 레벨마다 직전 레벨의 레벨업 기준 시간을 0%, 이번 레벨업 기준 시간을 100%로 계산한다 (#679)
 * - 100%는 레벨업 가능 시점과 같다 (CheckLevelUpConditionUseCase와 같은 minHoursForLevelUp 기준)
 * - 레벨업을 늦게 하면 0%보다 큰 값에서 시작한다 (레벨업 때 화분은 potLevel만 바뀌고 그 시점의 누적 시간은 저장하지 않음)
 * - 구간을 벗어난 값(기록 삭제로 0% 아래, 레벨업을 미뤄 100% 위)은 구간 안으로 자른다. 문구의 현재 시간도 같이 자른다
 * - 퍼센트는 버림 — 레벨업 가능 전에는 문구가 100%로 보이지 않는다
 * - 문구 예: "45% (1시간 48분/4시간)", 시간 표기는 #649 규칙
 */
internal fun buildLevelUpProgress(pot: Pot): LevelUpProgress {
    val condition = PotLevelConditions.getCondition(pot.potLevel)
    val startHours = PotLevelConditions.getProgressStartHours(pot.potLevel)
    if (condition == null || startHours == null) return LevelUpProgress(1f, MAX_LEVEL_PROGRESS_TEXT)

    val startMs = startHours * HOUR_MS
    // 구간은 항상 0보다 크다 — 기준 시간이 레벨마다 늘어나는지는 LevelUpProgressTest가 확인한다
    val spanMs = condition.minHoursForLevelUp * HOUR_MS - startMs
    val elapsedMs = (pot.potTotalStudyingTime - startMs).coerceIn(0L, spanMs)
    val percent = elapsedMs * 100 / spanMs

    val current = TimeFormatter.formatToHoursMinutes(elapsedMs)
    val required = TimeFormatter.formatToHoursMinutes(spanMs)
    return LevelUpProgress(
        progress = elapsedMs.toFloat() / spanMs,
        text = "$percent% ($current/$required)"
    )
}

private const val HOUR_MS = 3_600_000L
