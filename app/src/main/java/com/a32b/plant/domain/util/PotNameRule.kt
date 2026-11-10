package com.a32b.plant.domain.util

/**
 * 화분 이름 입력 규칙 — 화분 생성(NewBornTreeScreen)과 이름 변경(StudyPlanDetailScreen)이 함께 사용한다.
 * 두 화면이 같은 기준을 쓰도록 최대 길이를 한 곳에서 관리한다. (#360: 이름 변경에만 제한이 빠져 있었음)
 */
object PotNameRule {
    const val MAX_LENGTH = 15

    /**
     * 입력값을 저장할 형태로 바꾼다. 생성과 이름 변경 모두 저장 전에 이 값으로 바꿔서 저장한다. (#563)
     * - 앞뒤 공백 제거
     * - 가운데 공백("새  싹")은 그대로 둔다
     */
    fun normalize(name: String): String = name.trim()
}
