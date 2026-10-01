package com.a32b.plant.domain.model

data class ReportPotRank(
    val potId: String,
    val potName: String,
    val totalMillis: Long,
    val otherTiedCount: Int
)

data class ReportPotComparison(
    val highest: ReportPotRank?,
    val lowest: ReportPotRank?
)
