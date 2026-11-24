package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType

data class LevelUpCondition(
    val minHoursForLevelUp: Long,    // 레벨업 가능 최소 시간(누적) — 레벨업 버튼 활성화 기준, 홈 진행 바 100% 기준
    val requiredItems: Map<ItemType, Int>
)

object PotLevelConditions {
    val table: Map<Int, LevelUpCondition> = mapOf(
        0 to LevelUpCondition(
            minHoursForLevelUp = 1L,
            requiredItems = mapOf(ItemType.HEART to 1)
        ),
        1 to LevelUpCondition(
            minHoursForLevelUp = 5L,
            requiredItems = mapOf(
                ItemType.SUN to 3,
                ItemType.WATER to 1,
                ItemType.HEART to 5
            )
        ),
        2 to LevelUpCondition(
            minHoursForLevelUp = 15L,
            requiredItems = mapOf(
                ItemType.WATER to 5,
                ItemType.SUN to 15,
                ItemType.HEART to 22
            )
        ),
        3 to LevelUpCondition(
            minHoursForLevelUp = 30L,
            requiredItems = mapOf(
                ItemType.FERTILIZER to 3,
                ItemType.SUN to 13,
                ItemType.WATER to 4
            )
        ),
        4 to LevelUpCondition(
            minHoursForLevelUp = 70L,
            requiredItems = mapOf(
                ItemType.NUTRIENT to 1,
                ItemType.FERTILIZER to 10,
                ItemType.SUN to 17,
                ItemType.HEART to 30
            )
        ),
        5 to LevelUpCondition(
            minHoursForLevelUp = 150L,
            requiredItems = mapOf(
                ItemType.NUTRIENT to 5,
                ItemType.FERTILIZER to 10,
                ItemType.WATER to 35,
                ItemType.SUN to 30,
                ItemType.HEART to 50
            )
        )
    )

    // 최고 레벨: 다음 레벨로 올리는 조건이 있는 마지막 레벨 + 1 (firestore.rules의 maxPotLevel()과 같은 기준)
    val MAX_LEVEL: Int = table.keys.max() + 1

    fun getCondition(currentLevel: Int): LevelUpCondition? = table[currentLevel]

    /**
     * 홈 진행 바 0% 기준 시간(누적) — 직전 레벨의 레벨업 가능 최소 시간 (#679)
     * - Lv.0은 직전 레벨이 없으므로 0시간
     * - 최고 레벨(조건 없음)은 진행 바 구간이 없으므로 null
     */
    fun getProgressStartHours(currentLevel: Int): Long? {
        if (getCondition(currentLevel) == null) return null
        return table[currentLevel - 1]?.minHoursForLevelUp ?: 0L
    }
}
