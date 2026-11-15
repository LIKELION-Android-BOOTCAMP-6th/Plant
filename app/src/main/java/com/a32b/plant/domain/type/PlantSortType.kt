package com.a32b.plant.domain.type

enum class PlantSortType(val displayName: String) {
    CREATED_DATE("생성일"),
    TOTAL_STUDY_TIME("누적 학습 시간"),
    LEVEL("레벨"),
    NAME("이름"),
    RECENT_STUDY("최근 학습순")
}