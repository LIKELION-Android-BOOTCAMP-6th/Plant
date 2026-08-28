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
    //레벨 업 계산 -> 시간만 변경, 아이템 획득 여부는 별도 필요
    val level: String get(){
        if (id.isNullOrEmpty()) return "EMPTY"

        val rawMillis = potTotalStudyingTime ?: 0L
        val hours = rawMillis / 3600000.0
        val calculatedLevel =  when {
            hours >= 500.0 -> 6
            hours >= 200.0 -> 5
            hours >= 90.0 -> 4
            hours >= 40.0 -> 3
            hours >= 15.0 -> 2
            hours >= 5.0 -> 1
            else -> 0
        }
        return calculatedLevel.toString()
    }

    companion object {
        val EMPTY = Pot(
            id = "",
            tagId = "",
            tagName = "",
            name = "화분을 추가해주세요",
            imageUrl = "",
            potTotalStudyingTime = 0L,
            createdAt = null,
            completedAt = null,
            isCompleted = false
        )
    }
}