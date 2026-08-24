package com.a32b.plant.domain.model

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId


sealed interface AttendanceDecision {
    data class Success(
        val newCount: Int,
        val reward: AttendanceReward?
    ) : AttendanceDecision {
        val isMonthCompleted: Boolean
            get() = newCount >= MONTHLY_ATTENDANCE_LIMIT
    }

    data object AlreadyChecked : AttendanceDecision
    data object MonthCompleted : AttendanceDecision
}

private const val MONTHLY_ATTENDANCE_LIMIT = 28
private val KST_ZONE_ID = ZoneId.of("Asia/Seoul")
data class DailyCheckThisMonth(
    val count: Int,
    val lastCheckedAt: Long?,
) {
    fun decideNext(nowKst: LocalDate): AttendanceDecision {
        val lastDate = lastCheckedAt
            ?.takeIf { it > 0L }
            ?.let { Instant.ofEpochMilli(it).atZone(KST_ZONE_ID).toLocalDate() }
        val isSameMonth = lastDate != null &&
            YearMonth.from(lastDate) == YearMonth.from(nowKst)

        if (isSameMonth && count >= MONTHLY_ATTENDANCE_LIMIT) {
            return AttendanceDecision.MonthCompleted
        }

        if (lastDate == nowKst) {
            return AttendanceDecision.AlreadyChecked
        }

        val newCount = if (isSameMonth) count + 1 else 1
        if (newCount > MONTHLY_ATTENDANCE_LIMIT) {
            return AttendanceDecision.MonthCompleted
        }

        return AttendanceDecision.Success(
            newCount = newCount,
            reward = AttendanceRewardTable.of(newCount)
        )
    }
}

data class Item(
    val heart: Int,
    val sun: Int,
    val water: Int,
    val fertilizer: Int,
    val nutrient: Int,
    val box: Int,
)
data class User(
    val uid: String,
    val nickname: String,
    val profileImg: String,
    // [추가] 마지막으로 공부를 시작했던 화분의 ID
    val lastSelectedPotId: String,
    var isFirstLogin: Boolean?, // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 재설정 하고 false 바꾸기
    var isDarkMode: Boolean,
    val totalStudyTime: Long,
    val coin: Int,
    val monthCheck: DailyCheckThisMonth,
    val item: Item
    //===아이템===


) {
    companion object{
        fun create() = User(
            uid = "", //머지 후 수정 예정
            nickname = "",
            profileImg = "",
            lastSelectedPotId = "",
            isFirstLogin = true, // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 설정하면 false
            isDarkMode = false,
            totalStudyTime = 0L,
            coin = 0,
            monthCheck = DailyCheckThisMonth(
                count = 0,
                lastCheckedAt = null
            ),
            item = Item(0,0,0,0,0,0)

        )
    }
}
