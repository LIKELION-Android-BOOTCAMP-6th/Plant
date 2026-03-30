package com.a32b.plant.data.model

import com.google.firebase.firestore.PropertyName
import java.security.Timestamp

data class LogInfo(
    val id: String = "",
    val name: String = "",
    val tag: String = "",
    val level: String = "0",
    val imageUrl: String = "0",
    val potTotalStudyingTime: Long = 0L,
    val createdAt: Timestamp? = null,
    val completedAt: Timestamp? = null,
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false
)