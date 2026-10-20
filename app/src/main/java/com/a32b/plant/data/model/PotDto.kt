package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

data class PotDto(
    @DocumentId
    var id: String = "",
    @get:PropertyName("tag_id") @set:PropertyName("tag_id")
    var tagId: String = "",
    @get:PropertyName("tag_name") @set:PropertyName("tag_name")
    var tagName: String = "",
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",
    @get:PropertyName("potTotalStudyingTime") @set:PropertyName("potTotalStudyingTime")
    var potTotalStudyingTime: Long = 0L,
    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Timestamp? = null,
    @get:PropertyName("completedAt") @set:PropertyName("completedAt")
    var completedAt: Timestamp? = null,
    @get:PropertyName("isCompleted") @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,
    @get:PropertyName("lastStudiedAt") @set:PropertyName("lastStudiedAt")
    var lastStudiedAt: Timestamp? = null,
    @get:PropertyName("potLevel") @set:PropertyName("potLevel")
    var potLevel: Int = 0
)
