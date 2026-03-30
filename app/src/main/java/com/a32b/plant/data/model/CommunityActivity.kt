package com.a32b.plant.data.model

import com.google.firebase.Timestamp

data class CommunityActivity(
    val type: String = "",
    val title: String = "",
    val targetId: String = "",
    val comment: String? = null,
    val createAt: Timestamp = Timestamp.now()
)
