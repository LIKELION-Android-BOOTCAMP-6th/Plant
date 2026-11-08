package com.a32b.plant.domain.util

/**
 * 화분 이름 입력 규칙 — 화분 생성(NewBornTreeScreen)과 이름 변경(StudyPlanDetailScreen)이 함께 사용한다.
 * 두 화면이 같은 기준을 쓰도록 최대 길이를 한 곳에서 관리한다. (#360: 이름 변경에만 제한이 빠져 있었음)
 */
object PotNameRule {
    const val MAX_LENGTH = 15
}
