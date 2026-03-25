package com.a32b.plant.data.model
import com.google.firebase.firestore.PropertyName

data class UserProfile(
    val nickname: String? = null,
    val profileImg: String? = null,
    // [추가] 마지막으로 공부를 시작했던 화분의 ID
    val lastSelectedPotId: String? = null,
    @get:PropertyName("currentPot") @set:PropertyName("currentPot")
    var currentPot: PotInfo = PotInfo(),
    // [추가] 사용자가 보유한 화분 전체 리스트
    val potList: List<PotInfo> = emptyList(),
    val isFirstLogin: Boolean? = null,
    val isAutoLogin: Boolean? = null,
    val isDarkMode: Boolean? = null,
    val totalStudyTime: Long? = null,
    val completedPotsCount: Int? = null
)