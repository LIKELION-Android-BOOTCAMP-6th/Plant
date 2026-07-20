package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.data.model.CommunityActivityDto
import com.a32b.plant.domain.model.CommunityActivity

fun CommunityActivityDto.toDomain() : CommunityActivity = CommunityActivity(
    uid, type, title, targetId, comment, commentId, createAt.toLong()
)

fun CommunityActivity.toDto() : CommunityActivityDto = CommunityActivityDto(
    uid, type, title, targetId, comment, commentId, createAt.toTimestamp()

)