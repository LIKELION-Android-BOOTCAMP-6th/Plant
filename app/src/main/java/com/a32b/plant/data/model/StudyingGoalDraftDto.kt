package com.a32b.plant.data.model

data class StudyingGoalDraftDto(
    var userId: String = "",
    var potId: String = "",
    var logs: List<String> = emptyList()
)
