package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.UserDto
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.User

/**
 User <-> UserDto
 */

fun UserDto.toDomain(potLIst: List<Pot>) : User = User(
    nickname = nickname,
    profileImg = profileImg,
    lastSelectedPotId = lastSelectedPotId,
    isFirstLogin = isFirstLogin,
    isDarkMode = isDarkMode,
    totalStudyTime = totalStudyTime,
    potList = potLIst
)

fun User.toDto(): UserDto = UserDto(
    nickname = nickname,
    profileImg = profileImg,
    lastSelectedPotId = lastSelectedPotId,
    isFirstLogin = isFirstLogin,
    isDarkMode = isDarkMode,
    totalStudyTime = totalStudyTime
)