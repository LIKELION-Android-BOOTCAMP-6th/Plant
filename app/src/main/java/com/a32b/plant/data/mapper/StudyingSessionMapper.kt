package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.StudyingSessionDto
import com.a32b.plant.domain.model.StudyingSession

fun StudyingSessionDto.toDomain() = StudyingSession(
    userId = userId,
    tag = tag,
    title = title,
    potId = potId,
    time = time,
    log = log,
    startedAt = startedAt
)

fun StudyingSession.toDto() = StudyingSessionDto(
    userId = userId,
    tag = tag,
    title = title,
    potId = potId,
    time = time,
    log = log,
    startedAt = startedAt
)
