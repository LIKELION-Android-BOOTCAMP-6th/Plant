package com.a32b.plant.presentation.home.viewmodel

import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.Item
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.type.ItemType

/** 레벨업 다이얼로그의 재료 한 줄 (#678) */
data class LevelUpItemRequirement(
    val type: ItemType,
    val owned: Int,
    val required: Int
) {
    val isEnough: Boolean get() = owned >= required
    /** 더 모아야 하는 개수 (충분하면 0) */
    val lackingCount: Int get() = (required - owned).coerceAtLeast(0)
}

/**
 * 레벨업 확인 다이얼로그 표시용 상태 (#678)
 * 화분·보유 아이템이 바뀌면 HomeViewModel이 다시 만든다 (열린 사이 아이템 수가 바뀌어도 최신 값으로 표시)
 */
data class LevelUpDialogUiState(
    val potId: String,
    val currentLevel: Int,
    // 홈 아이템 칸과 같은 순서 (ItemType 선언 순서)
    val items: List<LevelUpItemRequirement>,
    val currentStudyTimeText: String,
    val requiredStudyTimeText: String,
    // null: 학습 시간 충족
    val remainingStudyTimeText: String?
) {
    val newLevel: Int get() = currentLevel + 1
    val isStudyTimeEnough: Boolean get() = remainingStudyTimeText == null
    /** 다이얼로그의 레벨업 버튼을 흐리게 할지 판단하는 기준 (서버 규칙·LevelUpPotUseCase가 최종으로 다시 확인한다) */
    val isReady: Boolean get() = isStudyTimeEnough && items.all { it.isEnough }

    /**
     * 조건이 부족할 때 레벨업 버튼을 누르면 보여줄 토스트 문구 — 충족하면 null
     * 학습 시간을 먼저 안내한다 (LevelUpPotUseCase의 확인 순서와 같음). 재료는 다이얼로그에 강조돼 있다
     * 재료만 부족하면 부족한 개수를 함께 보여준다 (예: "재료가 부족해요: 햇빛 2개, 물 1개")
     */
    val notReadyMessage: String?
        get() = when {
            remainingStudyTimeText != null -> "학습 시간이 $remainingStudyTimeText 더 필요해요."
            items.any { !it.isEnough } ->
                "재료가 부족해요: " + items.filter { !it.isEnough }.joinToString { "${it.type.kor} ${it.lackingCount}개" }
            else -> null
        }
}

/**
 * 화분과 보유 아이템으로 다이얼로그 상태를 만든다 — 완료 화분·최고 레벨은 레벨업할 수 없으므로 null
 * 학습 시간 기준은 레벨업 가능 최소 시간(minHoursForLevelUp)이다. 홈 진행 바 100% 기준과 같다 (#679)
 */
internal fun buildLevelUpDialogUiState(pot: Pot, item: Item): LevelUpDialogUiState? {
    if (pot.isCompleted) return null
    val condition = PotLevelConditions.getCondition(pot.potLevel) ?: return null

    val requiredMs = condition.minHoursForLevelUp * HOUR_MS
    val remainingMs = requiredMs - pot.potTotalStudyingTime

    return LevelUpDialogUiState(
        potId = pot.id,
        currentLevel = pot.potLevel,
        items = condition.requiredItems
            .map { (type, required) -> LevelUpItemRequirement(type, item.countOf(type), required) }
            .sortedBy { it.type.ordinal },
        currentStudyTimeText = TimeFormatter.formatToHoursMinutes(pot.potTotalStudyingTime),
        requiredStudyTimeText = TimeFormatter.formatToHoursMinutes(requiredMs),
        // 남은 시간은 분 단위로 올림 — 30초 남았을 때 "1분 미만 남았어요"가 아니라 "1분 남았어요"로 안내한다
        remainingStudyTimeText = if (remainingMs > 0) {
            TimeFormatter.formatToHoursMinutes((remainingMs + MINUTE_MS - 1) / MINUTE_MS * MINUTE_MS)
        } else {
            null
        }
    )
}

/**
 * 레벨업 성공 토스트 문구 (#678)
 * "Lv.2로", "Lv.3으로"처럼 숫자를 읽는 소리에 맞춰 조사를 고른다
 */
internal fun levelUpSuccessMessage(newLevel: Int, isPending: Boolean): String {
    val message = "Lv.$newLevel${directionParticle(newLevel)} 성장했어요!"
    return if (isPending) "$message 네트워크에 연결되면 자동으로 저장돼요." else message
}

/**
 * 숫자 뒤의 "(으)로" — 끝자리를 읽는 소리 기준
 * 받침이 있으면 "으로": 0(영)·3(삼)·6(육). 10(십)·100(백)처럼 끝자리가 0인 수도 받침이 있다
 * ㄹ 받침(1 일·7 칠·8 팔)과 받침 없음(2·4·5·9)은 "로"
 */
private fun directionParticle(number: Int): String = when (number % 10) {
    0, 3, 6 -> "으로"
    else -> "로"
}

private const val MINUTE_MS = 60_000L
private const val HOUR_MS = 60 * MINUTE_MS
