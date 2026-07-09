package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.TagDto
import com.a32b.plant.domain.model.Tag

fun TagDto.toDomain(): Tag = Tag(
    id = id,
    name = name,
    parentId = parentId,
    no = no
)

fun  Tag.toDto(): TagDto = TagDto(
    id = id,
    name = name,
    parentId = parentId,
    no = no
)