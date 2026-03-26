package com.a32b.plant.data.model
import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val nickname: String? = null,
    val profileImg: String? = null,
    // [추가] 마지막으로 공부를 시작했던 화분의 ID
    val lastSelectedPotId: String? = null,
    val currentPot: PotInfo = PotInfo(),
    // [추가] 사용자가 보유한 화분 전체 리스트
    val potList: List<PotInfo> = emptyList(),
    @get:PropertyName("isFirstLogin") @set:PropertyName("isFirstLogin")
    var isFirstLogin: Boolean? = null, // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 재설정 하고 false 바꾸기
    @get:PropertyName("isAutoLogin") @set:PropertyName("isAutoLogin")
    var isAutoLogin: Boolean? = null,
    @get:PropertyName("isDarkMode") @set:PropertyName("isDarkMode")
    var isDarkMode: Boolean? = null,
    val totalStudyTime: Long? = null,
    val completedPotsCount: Int? = null
)