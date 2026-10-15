package com.a32b.plant.domain.model

data class ReportPotLogs(
    val pot: Pot,
    val logs: List<StudyLog>
)
