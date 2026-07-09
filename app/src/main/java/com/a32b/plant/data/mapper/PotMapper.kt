package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.data.model.PotDto
import com.a32b.plant.domain.model.Pot

fun PotDto.toDomain(): Pot = Pot(
    id = id,
    tagId = tagId,
    tagName = tagName,
    name = name,
    imageUrl = imageUrl,
    potTotalStudyingTime = potTotalStudyingTime,
    createdAt = createdAt.toLong(),
    completedAt = completedAt.toLong(),
    isCompleted = isCompleted,
)

fun Pot.toDto(): PotDto = PotDto(
    id = id,
    tagId = tagId,
    tagName = tagName,
    name = name,
    imageUrl = imageUrl,
    potTotalStudyingTime = potTotalStudyingTime,
    createdAt = createdAt.toTimestamp(),
    completedAt = if(isCompleted) completedAt.toTimestamp() else null,
    isCompleted = isCompleted,
)