package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.DailyCheckThisMonthDto
import com.a32b.plant.data.model.ItemDto
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
    potList = potLIst,
    coin = coin,
    isDailyChecked = monthCheck.isDailyChecked,
    count = monthCheck.count,
    heart = item.heart,
    sun = item.sun,
    water = item.water,
    fertilizer = item.fertilizer,
    nutrient = item.nutrient,
    box = item.box
)

fun User.toDto(): UserDto = UserDto(
    nickname = nickname,
    profileImg = profileImg,
    lastSelectedPotId = lastSelectedPotId,
    isFirstLogin = isFirstLogin,
    isDarkMode = isDarkMode,
    totalStudyTime = totalStudyTime,
    coin = coin,
    monthCheck = DailyCheckThisMonthDto(isDailyChecked, count),
    item = ItemDto(heart, sun, water, fertilizer, nutrient, box)
)