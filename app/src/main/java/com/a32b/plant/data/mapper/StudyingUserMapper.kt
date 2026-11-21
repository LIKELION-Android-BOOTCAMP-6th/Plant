package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.data.model.StudyingUserDto
import com.a32b.plant.domain.model.StudyingUser

fun StudyingUserDto.toDomain() : StudyingUser = StudyingUser(
    uid, nickname, profileImg, tag, studyingTime,
    updatedAt = updatedAt?.toLong()
)

fun StudyingUser.toDto() : StudyingUserDto = StudyingUserDto(
    uid, nickname, profileImg, tag, studyingTime
)