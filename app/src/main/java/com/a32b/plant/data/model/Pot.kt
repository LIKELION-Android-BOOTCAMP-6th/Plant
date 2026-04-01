package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName
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
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false
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
}