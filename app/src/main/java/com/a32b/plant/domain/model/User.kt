package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType
import java.time.LocalDate


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

/**
 * 이번 달 출석 정보.
 * 출석 판정·저장·보상 지급은 서버 함수 checkAttendance(functions/index.js)가 한다 (#531).
 * 앱은 출석판 표시에만 쓴다.
 *
 * @param lastCheckedDate 서버가 서버 시각 기준으로 계산해 저장한 마지막 출석일 (KST, "yyyy-MM-dd").
 *   출석한 적 없으면 null
 * @param lastCheckedAt 마지막 출석 시각 (기록용). 판정에는 쓰지 않는다
 */
data class DailyCheckThisMonth(
    val count: Int,
    val lastCheckedAt: Long?,
    val lastCheckedDate: String?,
) {
    /**
     * 출석판 표시용으로, [todayKst]에 출석하면 어떤 결과가 될지 계산한다.
     * 서버 decideAttendance와 같은 기준(날짜 키 비교, 앞 7자리로 같은 달 확인)을 쓴다.
     * 실제 결과는 서버가 서버 시각으로 정하므로, 기기 날짜가 틀리면 표시만 다를 수 있다.
     */
    fun decideNext(todayKst: LocalDate): AttendanceDecision {
        val todayKey = todayKst.toString() // ISO-8601 "yyyy-MM-dd"
        val isSameMonth = lastCheckedDate != null &&
            lastCheckedDate.take(7) == todayKey.take(7)
        // 달이 바뀌었으면 지난달 횟수는 보지 않는다 (이번 출석이 1회차)
        val currentCount = if (isSameMonth) count else 0

        if (currentCount >= MONTHLY_ATTENDANCE_LIMIT) {
            return AttendanceDecision.MonthCompleted
        }

        if (lastCheckedDate == todayKey) {
            return AttendanceDecision.AlreadyChecked
        }

        val newCount = currentCount + 1
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
) {
    /** 아이템 종류별 보유 개수 (골드는 아이템이 아니므로 0) */
    fun countOf(type: ItemType): Int = when (type) {
        ItemType.HEART -> heart
        ItemType.SUN -> sun
        ItemType.WATER -> water
        ItemType.FERTILIZER -> fertilizer
        ItemType.NUTRIENT -> nutrient
        ItemType.BOX -> box
        ItemType.GOLD_300, ItemType.GOLD_500, ItemType.GOLD_1000 -> 0
    }
}
data class User(
    val uid: String,
    val nickname: String,
    val profileImg: String,
    // [추가] 마지막으로 공부를 시작했던 화분의 ID
    val lastSelectedPotId: String,
    var isFirstLogin: Boolean?, // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 재설정 하고 false 바꾸기
    val totalStudyTime: Long,
    val gold: Int,
    val monthCheck: DailyCheckThisMonth,
    val item: Item,
    val activeSessionToken: String?,
    val unlockedProfileLevel: Int
) {
    companion object{
        fun create() = User(
            uid = "", //머지 후 수정 예정
            nickname = "",
            profileImg = "",
            lastSelectedPotId = "",
            isFirstLogin = true, // 회원가입 시 true 유지 -> 첫 로그인 후 닉네임 설정하면 false
            totalStudyTime = 0L,
            gold = 0,
            monthCheck = DailyCheckThisMonth(
                count = 0,
                lastCheckedAt = null,
                lastCheckedDate = null
            ),
            item = Item(0,0,0,0,0,0),
            activeSessionToken = null,
            unlockedProfileLevel = 0
        )
    }
}
