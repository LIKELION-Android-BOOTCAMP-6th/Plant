package com.a32b.plant.data.mapper

import com.a32b.plant.data.model.AttendanceResultDto
import com.a32b.plant.data.model.AttendanceRewardDto
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.type.ItemType

// 골드는 ItemType에 금액별로 여러 개라 fieldKey("gold")로 찾으면 안 된다 → item 보상에서는 제외
private val ATTENDANCE_ITEM_TYPES = ItemType.entries.filter { it.fieldKey != "gold" }

/**
 * 서버 응답을 [AttendanceDecision]으로 바꾼다.
 * 알 수 없는 status이거나 SUCCESS인데 newCount가 없으면 null (Repository가 서버 오류로 처리)
 */
fun AttendanceResultDto.toDomain(): AttendanceDecision? = when (status) {
    "SUCCESS" -> newCount?.takeIf { it > 0 }?.let { count ->
        AttendanceDecision.Success(newCount = count, reward = reward?.toDomain())
    }
    "ALREADY_CHECKED" -> AttendanceDecision.AlreadyChecked
    "MONTH_COMPLETED" -> AttendanceDecision.MonthCompleted
    else -> null
}

/**
 * 알 수 없는 보상은 null (지급은 서버에서 이미 끝났으므로 보상 안내만 생략한다.
 * 서버에 새 아이템이 추가돼도 출석 처리 자체는 성공으로 보여준다)
 */
private fun AttendanceRewardDto.toDomain(): AttendanceReward? {
    gold?.takeIf { it > 0 }?.let { return AttendanceReward.Coin(it) }
    val type = ATTENDANCE_ITEM_TYPES.find { it.fieldKey == item } ?: return null
    val count = amount?.takeIf { it > 0 } ?: return null
    return AttendanceReward.ItemReward(type, count)
}
