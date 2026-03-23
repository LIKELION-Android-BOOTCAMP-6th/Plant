package com.a32b.plant.data.model
import com.google.firebase.firestore.PropertyName

// 화분 정보 모델
data class PotInfo(
    val id: String = "",
    val tag: String = "",
    val name: String = "",
    val level: String = ""
)

// 유저 프로필 모델 (Firestore 구조와 매핑)
data class UserProfile(
    val nickname: String = "",
    val profileImg: String = "",
    @get:PropertyName("currentPot") @set:PropertyName("currentPot")
    var currentPot: PotInfo = PotInfo(),
    val isFirstLogin: Boolean = true,
    val isAutoLogin: Boolean = false,
    val isDarkMode: Boolean = false,
    val totalStudyTime: Long = 0L,
    val completedPotsCount: Int = 0
)