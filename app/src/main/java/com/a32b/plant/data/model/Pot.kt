package com.a32b.plant.data.model

//화분 정보 모음
data class PotInfo(
    val id: String = "",
    val tag: String = "",
    val name: String = "",
    val level: String = "",
    val imageUrl: String = "",
    val todayStudyingTime: Long = 0L
)