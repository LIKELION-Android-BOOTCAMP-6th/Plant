package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.BonusBoxOpeningDto
import com.a32b.plant.domain.type.ItemType
import org.junit.Assert.assertEquals
import org.junit.Test

class BonusBoxMapperTest {

    // 서버 확률 표(functions/index.js BONUS_BOX_DROP_TABLE)의 type 이름이 모두 ItemType으로 바뀌는지
    @Test
    fun mapsEveryServerDropName() {
        val serverNames = listOf(
            "HEART", "SUN", "WATER", "FERTILIZER", "NUTRIENT", "BOX", "GOLD_300", "GOLD_500", "GOLD_1000"
        )

        val result = BonusBoxOpeningDto(items = serverNames).toDomain()

        assertEquals(ItemType.entries.toList(), result)
    }

    // 같은 보상이 여러 번 나오면 그대로 여러 개로 유지 (결과 화면이 개수를 센다)
    @Test
    fun keepsDuplicatesInOrder() {
        val result = BonusBoxOpeningDto(items = listOf("HEART", "BOX", "HEART")).toDomain()

        assertEquals(listOf(ItemType.HEART, ItemType.BOX, ItemType.HEART), result)
    }

    // 앱이 모르는 이름(서버에 새로 추가된 보상)은 건너뛴다
    @Test
    fun skipsUnknownNames() {
        val result = BonusBoxOpeningDto(items = listOf("NEW_ITEM", "SUN")).toDomain()

        assertEquals(listOf(ItemType.SUN), result)
    }
}
