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
    // DB의 'currnetPot'을 코드 상에서는 'currentPot'으로 매핑
    @get:PropertyName("currnetPot") @set:PropertyName("currnetPot")
    var currentPot: PotInfo = PotInfo(),
    val isAutoLogin: Boolean = false,
    val isDarkMode: Boolean = false
)