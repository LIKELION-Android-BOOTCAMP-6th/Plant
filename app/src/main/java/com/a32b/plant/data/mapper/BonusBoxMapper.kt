package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.BonusBoxOpeningDto
import com.a32b.plant.domain.type.ItemType

/**
 * 서버가 보낸 이름을 [ItemType]으로 바꾼다.
 * 알 수 없는 이름은 건너뛴다 (서버에 새 보상이 추가돼도 앱이 멈추지 않게. 지급은 서버에서 이미 끝남)
 */
fun BonusBoxOpeningDto.toDomain(): List<ItemType> =
    items.mapNotNull { name -> ItemType.entries.find { it.name == name } }
