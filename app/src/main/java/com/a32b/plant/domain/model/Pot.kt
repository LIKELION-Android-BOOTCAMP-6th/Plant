package com.a32b.plant.domain.model

/**
PotInfo -> Pot으로 변경
 */
//화분 정보 모음
data class Pot(
    val id: String,
    val tagId: String,
    val tagName: String,
    val name: String,
    val potTotalStudyingTime: Long,
    val createdAt: Long?,
    val completedAt: Long?,
    val isCompleted: Boolean,
    val lastStudiedAt: Long?,
    val potLevel: Int,
    ) {

    companion object {
        val EMPTY = Pot(
            id = "",
            tagId = "",
            tagName = "",
            name = "화분을 추가해주세요",
            potTotalStudyingTime = 0L,
            createdAt = null,
            completedAt = null,
            isCompleted = false,
            lastStudiedAt = null,
            potLevel = 0
        )
    }
}