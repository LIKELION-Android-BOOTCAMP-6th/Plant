package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

//화분 정보 모음
data class PotInfo(
    val id: String? = null,
    val tag: String? = null,
    val name: String? = null,
    val imageUrl: String? = null,
    val potTotalStudyingTime: Long? = null,
    @ServerTimestamp
    val createdAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    val isCompleted: Boolean = false
){
    //레벨 업 계산
    val level: String get(){
        if (id.isNullOrEmpty()) return "EMPTY"

        val rawMillis = potTotalStudyingTime ?: 0L

        val totalSeconds = if (rawMillis > 1_000_000L) rawMillis / 1000 else rawMillis
        val hours = totalSeconds / 3600
        val calculatedLevel =  when {
            hours >= 77 -> 5
            hours >= 50 -> 4
            hours >= 30 -> 3
            hours >= 10 -> 2
            hours >= 3 -> 1
            else -> 0
        }
        return calculatedLevel.toString()
    }

}
data class Logs(
    val title: String = "",
    val content: List<String> = emptyList(),
    val studyTime: Long = 0L,
    @ServerTimestamp
    val createdAt: Timestamp? = null
)