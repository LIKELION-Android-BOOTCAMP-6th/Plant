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
    val imageUrl: String,
    val potTotalStudyingTime: Long,
    val createdAt: Long?,
    val completedAt: Long?,
    val isCompleted: Boolean
){
    //레벨 업 계산
    val level: String get(){
        if (id.isNullOrEmpty()) return "EMPTY"

        val rawMillis = potTotalStudyingTime ?: 0L
        val hours = rawMillis / 3600000.0
        val calculatedLevel =  when {
            hours >= 77.0 -> 5
            hours >= 50.0 -> 4
            hours >= 30.0 -> 3
            hours >= 10.0 -> 2
            hours >= 3.0 -> 1
            else -> 0
        }
        return calculatedLevel.toString()
    }

    companion object {
        fun isEmpty() = Pot(
            id = "",
            tagId = "",
            tagName = "",
            name = "화분을 추가해주세요.",
            imageUrl = "",
            potTotalStudyingTime = 0L,
            createdAt = null,
            completedAt = null,
            isCompleted = false
        )
    }
}