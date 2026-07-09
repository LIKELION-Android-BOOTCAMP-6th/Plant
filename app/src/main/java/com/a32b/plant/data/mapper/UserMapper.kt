package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.DailyCheckThisMonthDto
import com.a32b.plant.data.model.ItemDto
import com.a32b.plant.data.model.UserDto
import com.a32b.plant.domain.model.DailyCheckThisMonth
import com.a32b.plant.domain.model.Item
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.User

/**
 User <-> UserDto
 */
fun DailyCheckThisMonthDto.toDomain() : DailyCheckThisMonth = DailyCheckThisMonth(
    isDailyChecked, count
)
fun DailyCheckThisMonth.toDto() : DailyCheckThisMonthDto = DailyCheckThisMonthDto(
    isDailyChecked, count
)

fun ItemDto.toDomain() : Item = Item(
    heart, sun, water, fertilizer, nutrient, box
)
fun Item.toDto() : ItemDto = ItemDto(
    heart, sun, water, fertilizer, nutrient, box
)

fun UserDto.toDomain(potLIst: List<Pot>) : User = User(
    nickname = nickname,
    profileImg = profileImg,
    lastSelectedPotId = lastSelectedPotId,
    isFirstLogin = isFirstLogin,
    isDarkMode = isDarkMode,
    totalStudyTime = totalStudyTime,
    potList = potLIst,
    coin = coin,
    monthCheck = monthCheck.toDomain(),
    item = item.toDomain()
)

fun User.toDto(): UserDto = UserDto(
    nickname = nickname,
    profileImg = profileImg,
    lastSelectedPotId = lastSelectedPotId,
    isFirstLogin = isFirstLogin,
    isDarkMode = isDarkMode,
    totalStudyTime = totalStudyTime,
    coin = coin,
    monthCheck = monthCheck.toDto(),
    item = item.toDto()
)