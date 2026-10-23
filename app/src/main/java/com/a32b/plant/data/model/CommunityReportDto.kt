package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId

data class CommunityReportDto(
    @DocumentId var id: String = "",
    var targetType: String = "",
    var targetId: String = "",
    var reporterId: String = "",
    var reason: String = "",
    var detail: String? = null,
    var createdAt: Timestamp? = null,
    var status: String = "pending"
)
