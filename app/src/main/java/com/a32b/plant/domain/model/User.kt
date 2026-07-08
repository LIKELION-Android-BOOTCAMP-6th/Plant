package com.a32b.plant.domain.model

/**
 UserProfile -> User로 변경
 currentPot, completedPotsCount 제거
 */
data class User(
    val nickname: String,
    val profileImg: String,
    // [추가] 마지막으로 공부를 시작했던 화분의 ID
    val lastSelectedPotId: String,
    // [추가] 사용자가 보유한 화분 전체 리스트
    val potList: List<Pot> = emptyList(),
    var isFirstLogin: Boolean?, // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 재설정 하고 false 바꾸기
    var isDarkMode: Boolean,
    val totalStudyTime: Long
) {
    companion object{
        fun create() = User(
            nickname = "",
            profileImg = "",
            lastSelectedPotId = "",
            isFirstLogin = true, // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 설정하면 false
            isDarkMode = false,
            totalStudyTime = 0L
        )
    }
}