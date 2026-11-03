package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType

data class LevelUpCondition(
    val minHoursForLevelUp: Long,    // 레벨업 가능 최소 시간 — 레벨업 버튼 활성화 기준[cite: 5]
    val maxHoursForProgress: Long,   // 진행도 바 기준 최대 시간 (필요 학습 시간)[cite: 5]
    val requiredItems: Map<ItemType, Int>
)

object PotLevelConditions {
    val table: Map<Int, LevelUpCondition> = mapOf(
        0 to LevelUpCondition(
            minHoursForLevelUp = 1L,
            maxHoursForProgress = 5L,
            requiredItems = mapOf(ItemType.HEART to 1)
        ),
        1 to LevelUpCondition(
            minHoursForLevelUp = 5L,
            maxHoursForProgress = 15L,
            requiredItems = mapOf(
                ItemType.SUN to 3,
                ItemType.WATER to 1,
                ItemType.HEART to 5
            )
        ),
        2 to LevelUpCondition(
            minHoursForLevelUp = 15L,
            maxHoursForProgress = 40L,
            requiredItems = mapOf(
                ItemType.WATER to 5,
                ItemType.SUN to 15,
                ItemType.HEART to 22
            )
        ),
        3 to LevelUpCondition(
            minHoursForLevelUp = 30L,
            maxHoursForProgress = 90L,
            requiredItems = mapOf(
                ItemType.FERTILIZER to 3,
                ItemType.SUN to 13,
                ItemType.WATER to 4
            )
        ),
        4 to LevelUpCondition(
            minHoursForLevelUp = 70L,
            maxHoursForProgress = 200L,
            requiredItems = mapOf(
                ItemType.NUTRIENT to 1,
                ItemType.FERTILIZER to 10,
                ItemType.SUN to 17,
                ItemType.HEART to 30
            )
        ),
        5 to LevelUpCondition(
            minHoursForLevelUp = 150L,
            maxHoursForProgress = 500L,
            requiredItems = mapOf(
                ItemType.NUTRIENT to 5,
                ItemType.FERTILIZER to 10,
                ItemType.WATER to 35,
                ItemType.SUN to 30,
                ItemType.HEART to 50
            )
        )
    )

    fun getCondition(currentLevel: Int): LevelUpCondition? = table[currentLevel]
}
