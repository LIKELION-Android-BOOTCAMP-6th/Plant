package com.a32b.plant.domain.util

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.type.PotSortType
import java.text.Collator
import java.util.Locale

fun List<Pot>.sortedByType(sortType: PotSortType): List<Pot> {
    return when (sortType) {
        PotSortType.CREATED_DATE -> this.sortedWith(
            compareByDescending<Pot> { it.createdAt != null }
                .thenByDescending { it.createdAt }
        )
        PotSortType.TOTAL_STUDY_TIME -> this.sortedWith(
            compareByDescending<Pot> { it.potTotalStudyingTime }
                .thenByDescending { it.createdAt }
        )
        PotSortType.LEVEL -> this.sortedWith(
            compareByDescending<Pot> { it.potLevel }
                .thenByDescending { it.createdAt }
        )
        PotSortType.NAME -> {
            val koreanCollator = Collator.getInstance(Locale.KOREAN)
            this.sortedWith(
                compareBy<Pot> { nameGroup(it.name) }
                    .then(compareBy(koreanCollator) { it.name })
            )
        }
        PotSortType.RECENT_STUDY -> this.sortedWith(
            compareByDescending<Pot> { it.lastStudiedAt != null }
                .thenByDescending { it.lastStudiedAt }
                .thenByDescending { it.createdAt }
        )
    }
}

// 정렬 그룹 분류
private fun nameGroup(name: String): Int {
    val classifications = name.firstOrNull() ?: return 3
    return when {
        classifications in '가' .. '힣' || classifications in 'ㄱ'..'ㅣ' -> 0 // 한글
        classifications in 'a'..'z' || classifications in 'A'..'Z' -> 1 // 영어
        classifications.isDigit() -> 2 // 숫자
        else -> 3 // 이모지 or 기호 등
    }
}