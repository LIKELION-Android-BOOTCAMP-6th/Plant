package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ReportReason

data class CommunityReport(
    val id: String,
    val targetType: String,
    val targetId: String,
    val reporterId: String,
    val reason: ReportReason,
    val detail: String?,
    val createdAt: Long?,
    val status: String
)
