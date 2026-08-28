package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.data.model.DailyCheckThisMonthDto
import com.a32b.plant.data.model.ItemDto
import com.a32b.plant.data.model.UserDto
import com.a32b.plant.domain.model.DailyCheckThisMonth
import com.a32b.plant.domain.model.Item
import com.a32b.plant.domain.model.User

/**
 User <-> UserDto
 */
fun DailyCheckThisMonthDto.toDomain() : DailyCheckThisMonth = DailyCheckThisMonth(
    count = count,
    // Firestore에 값이 없어도 0L로 들어옴 (진짜 null이 아님).
    // "출석한 적 있는지" 판단할 땐 이 필드로 null 비교하지 말 것.
    lastCheckedAt = lastCheckedAt.toLong()
)
fun DailyCheckThisMonth.toDto() : DailyCheckThisMonthDto = DailyCheckThisMonthDto(
    count = count,
    lastCheckedAt = lastCheckedAt?.toTimestamp()
)

fun ItemDto.toDomain() : Item = Item(
    heart, sun, water, fertilizer, nutrient, box
)
fun Item.toDto() : ItemDto = ItemDto(
    heart, sun, water, fertilizer, nutrient, box
)

fun UserDto.toDomain() : User = User(
    uid = uid,
    nickname = nickname,
    profileImg = profileImg,
    lastSelectedPotId = lastSelectedPotId,
    isFirstLogin = isFirstLogin,
    isDarkMode = isDarkMode,
    totalStudyTime = totalStudyTime,
    coin = coin,
    monthCheck = monthCheck.toDomain(),
    item = item.toDomain()
)

fun User.toDto(): UserDto = UserDto(
    uid = uid,
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