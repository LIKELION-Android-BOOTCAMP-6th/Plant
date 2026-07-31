package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.domain.model.StudyLog

fun StudyLogDto.toDomain() : StudyLog = StudyLog(
    title = title,
    contents = contents,
    studyingTime = studyingTime,
    createAt = createAt.toDate().time,
    id = id,
    isSelected = false
)

fun StudyLog.toDto() : StudyLogDto = StudyLogDto(
    title = title,
    contents = contents,
    studyingTime = studyingTime,
    createAt = com.google.firebase.Timestamp(createAt?.let { it / 1000 } ?: 0, 0),
    id = id,
)