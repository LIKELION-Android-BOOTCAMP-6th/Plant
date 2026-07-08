package com.a32b.plant.data.model

import com.google.firebase.firestore.PropertyName

// TODO: 화분 목록은 따로 받아온 후 매퍼로 변환 시 넣어주기

data class UserDto(
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
    val coin: Int = 0,
    @get:PropertyName("dailyCheckThisMonth") @set:PropertyName("dailyCheckThisMonth")
    var monthCheck: DailyCheckThisMonthDto = DailyCheckThisMonthDto(),
    @get:PropertyName("item") @set:PropertyName("item")
    var item: ItemDto = ItemDto()


)

data class DailyCheckThisMonthDto(
    @get:PropertyName("isDailyChecked") @set:PropertyName("isDailyChecked")
    var isDailyChecked: Boolean = false,
    val count: Int = 0
)

data class ItemDto(
    val heart: Int = 0,
    val sun: Int = 0,
    val water: Int = 0,
    val fertilizer: Int = 0,
    val nutrient: Int = 0,
    val box: Int = 0,
)