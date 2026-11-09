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
    status = StudyingSessionStatus.entries.find { it.name == status } ?: StudyingSessionStatus.STUDYING,
    // 이전 버전 백업: 세션 ID는 복구 시 새로 만들고, 완료 상태는 목표 개수만큼 미완료로 채운다 (StudyingSession.completedForLog)
    sessionId = sessionId.orEmpty(),
    completed = completed.orEmpty()
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
    status = status.name,
    sessionId = sessionId,
    completed = completed
)
