package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.domain.model.StudyLog

fun StudyLogDto.toDomain() : StudyLog = StudyLog(
    title = title,
    contents = contents,
    studyingTime = studyingTime,
    createAt = createAt.toLong(),
    id = id,
    isSelected = isSelected,
)

fun StudyLog.toDto() : StudyLogDto = StudyLogDto(
    title = title,
    contents = contents,
    studyingTime = studyingTime,
    createAt = createAt.toTimestamp(),
    id = id,
    isSelected = isSelected,
)