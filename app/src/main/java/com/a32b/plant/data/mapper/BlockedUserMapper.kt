package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.BlockedUserDto
import com.a32b.plant.domain.model.BlockedUser

fun BlockedUserDto.toDomain(): BlockedUser = BlockedUser(
    uid = uid,
    nickname = nickname,
    profileImg = profileImg,
    createdAt = createdAt?.toDate()?.time
)
