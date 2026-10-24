package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType

data class LevelUpCondition(
    val requiredHours: Long,       // 아이템 소비 기준 시점 — 레벨업 버튼 활성화 기준
    val requiredMaxHours: Long,    // 필요 학습 시간
    val requiredItems: Map<ItemType, Int>
)

object PotLevelConditions {
    val table: Map<Int, LevelUpCondition> = mapOf(
        0 to LevelUpCondition(
            requiredHours = 1L,
            requiredMaxHours = 5L,
            requiredItems = mapOf(ItemType.HEART to 1)
        ),
        1 to LevelUpCondition(
            requiredHours = 5L,
            requiredMaxHours = 15L,
            requiredItems = mapOf(
                ItemType.SUN to 3,
                ItemType.WATER to 1,
                ItemType.HEART to 5
            )
        ),
        2 to LevelUpCondition(
            requiredHours = 15L,
            requiredMaxHours = 40L,
            requiredItems = mapOf(
                ItemType.WATER to 5,
                ItemType.SUN to 15,
                ItemType.HEART to 22
            )
        ),
        3 to LevelUpCondition(
            requiredHours = 30L,
            requiredMaxHours = 90L,
            requiredItems = mapOf(
                ItemType.FERTILIZER to 3,
                ItemType.SUN to 13,
                ItemType.WATER to 4
            )
        ),
        4 to LevelUpCondition(
            requiredHours = 70L,
            requiredMaxHours = 200L,
            requiredItems = mapOf(
                ItemType.NUTRIENT to 1,
                ItemType.FERTILIZER to 10,
                ItemType.SUN to 17,
                ItemType.HEART to 30
            )
        ),
        5 to LevelUpCondition(
            requiredHours = 150L,
            requiredMaxHours = 500L,
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