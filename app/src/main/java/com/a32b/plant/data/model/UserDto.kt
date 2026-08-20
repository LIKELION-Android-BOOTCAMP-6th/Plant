package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// TODO: 화분 목록은 따로 받아온 후 매퍼로 변환 시 넣어주기

data class UserDto(
    @DocumentId
    var uid: String = "",
    @get:PropertyName("nickname") @set:PropertyName("nickname")
    var nickname: String = "",
    @get:PropertyName("profileImg") @set:PropertyName("profileImg")
    var profileImg: String = "",
    // [추가] 마지막으로 공부를 시작했던 화분의 ID
    @get:PropertyName("lastSelectedPotId") @set:PropertyName("lastSelectedPotId")
    var lastSelectedPotId: String = "",
    @get:PropertyName("isFirstLogin") @set:PropertyName("isFirstLogin")
    var isFirstLogin: Boolean? = null, // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 재설정 하고 false 바꾸기
    @get:PropertyName("isDarkMode") @set:PropertyName("isDarkMode")
    var isDarkMode: Boolean = false,
    @get:PropertyName("totalStudyTime") @set:PropertyName("totalStudyTime")
    var totalStudyTime: Long = 0L,
    @get:PropertyName("coin") @set:PropertyName("coin")
    var coin: Int = 0,
    @get:PropertyName("dailyCheckThisMonth") @set:PropertyName("dailyCheckThisMonth")
    var monthCheck: DailyCheckThisMonthDto = DailyCheckThisMonthDto(),
    @get:PropertyName("item") @set:PropertyName("item")
    var item: ItemDto = ItemDto()


)

data class DailyCheckThisMonthDto(
    @get:PropertyName("count") @set:PropertyName("count")
    var count: Int = 0,
    @get:PropertyName("lastCheckedAt") @set:PropertyName("lastCheckedAt")
    var lastCheckedAt: Timestamp? = null
)

data class ItemDto(
    @get:PropertyName("heart") @set:PropertyName("heart")
    var heart: Int = 0,
    @get:PropertyName("sun") @set:PropertyName("sun")
    var sun: Int = 0,
    @get:PropertyName("water") @set:PropertyName("water")
    var water: Int = 0,
    @get:PropertyName("fertilizer") @set:PropertyName("fertilizer")
    var fertilizer: Int = 0,
    @get:PropertyName("nutrient") @set:PropertyName("nutrient")
    var nutrient: Int = 0,
    @get:PropertyName("box") @set:PropertyName("box")
    var box: Int = 0,
)
