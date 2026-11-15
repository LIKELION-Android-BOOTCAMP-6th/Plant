package com.a32b.plant.domain.util

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.type.PlantSortType
import java.text.Collator
import java.util.Locale

fun List<Pot>.sortedBy(sortType: PlantSortType): List<Pot> {
    return when (sortType) {
        PlantSortType.CREATED_DATE -> this.sortedWith(
            compareByDescending<Pot> { it.createdAt != null }
                .thenByDescending { it.createdAt }
        )
        PlantSortType.TOTAL_STUDY_TIME -> this.sortedByDescending { it.potTotalStudyingTime }
        PlantSortType.LEVEL -> this.sortedByDescending { it.potLevel }
        PlantSortType.NAME -> {
            val koreanCollator = Collator.getInstance(Locale.KOREAN)
            this.sortedWith(compareBy(koreanCollator) { it.name })
        }
        PlantSortType.RECENT_STUDY -> this.sortedWith(
            compareByDescending<Pot> { it.lastStudiedAt != null }
                .thenByDescending { it.lastStudiedAt }
        )
    }
}