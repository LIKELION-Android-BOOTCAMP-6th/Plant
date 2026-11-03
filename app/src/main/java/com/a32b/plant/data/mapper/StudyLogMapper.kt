package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.domain.model.StudyLog

fun StudyLogDto.toDomain() : StudyLog = StudyLog(
    title = title,
    contents = contents,
    // 길이가 모자라면 미완료(false)로 채우고, 길어도 contents 길이에 맞춰 자른다 (기존 데이터 하위 호환)
    completedContents = List(contents.size) { index -> completedContents.getOrNull(index) ?: false },
    studyingTime = studyingTime,
    createAt = createAt.toLong(),
    startedAt = startedAt.toLong(),
    id = id,
    isSelected = false
)

fun StudyLog.toDto() : StudyLogDto = StudyLogDto(
    title = title,
    contents = contents,
    completedContents = completedContents,
    studyingTime = studyingTime,
    createAt = createAt.toTimestamp(),
    startedAt = startedAt.toTimestamp(),
    id = id,
)
