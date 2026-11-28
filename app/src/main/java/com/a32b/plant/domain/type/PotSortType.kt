package com.a32b.plant.domain.type

enum class PotSortType(val displayName: String) {
    CREATED_DATE("생성일"),
    TOTAL_STUDY_TIME("누적 학습 시간"),
    LEVEL("레벨"),
    NAME("이름"),
    RECENT_STUDY("최근 학습순");

    companion object {
        val DEFAULT = CREATED_DATE

        /** 저장된 이름(name)을 되돌린다. 없거나 지금 항목에 없는 이름(바뀌거나 지워진 항목)이면 [DEFAULT] (#803) */
        fun fromName(name: String?): PotSortType = entries.firstOrNull { it.name == name } ?: DEFAULT
    }
}