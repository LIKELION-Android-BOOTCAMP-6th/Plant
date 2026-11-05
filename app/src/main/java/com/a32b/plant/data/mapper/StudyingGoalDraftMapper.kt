package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.StudyingGoalDraftDto
import com.a32b.plant.domain.model.StudyingGoalDraft

fun StudyingGoalDraftDto.toDomain() = StudyingGoalDraft(
    userId = userId,
    potId = potId,
    logs = logs
)

fun StudyingGoalDraft.toDto() = StudyingGoalDraftDto(
    userId = userId,
    potId = potId,
    logs = logs
)
