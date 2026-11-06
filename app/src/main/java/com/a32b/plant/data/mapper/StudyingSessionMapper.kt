package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.StudyingSessionDto
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.type.StudyingSessionStatus

fun StudyingSessionDto.toDomain() = StudyingSession(
    userId = userId,
    tag = tag,
    title = title,
    potId = potId,
    time = time,
    log = log,
    startedAt = startedAt,
    isPaused = isPaused,
    // 값이 없거나 알 수 없는 값이면 공부 중으로 처리 (이전 버전 백업은 비정상 종료 복구 대상)
    status = StudyingSessionStatus.entries.find { it.name == status } ?: StudyingSessionStatus.STUDYING
)

fun StudyingSession.toDto() = StudyingSessionDto(
    userId = userId,
    tag = tag,
    title = title,
    potId = potId,
    time = time,
    log = log,
    startedAt = startedAt,
    isPaused = isPaused,
    status = status.name
)
