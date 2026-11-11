package com.a32b.plant.data.model

/**
 * 출석 체크 함수(functions/index.js checkAttendance)의 응답 (#531)
 *
 * - status: "SUCCESS" / "ALREADY_CHECKED" / "MONTH_COMPLETED"
 * - newCount: 이번 출석으로 바뀐 이번 달 출석 수 (SUCCESS일 때만)
 * - reward: 이번 출석으로 지급된 보상 (SUCCESS이고 보상이 있는 날만). 지급은 서버가 이미 끝냈다
 */
data class AttendanceResultDto(
    var status: String = "",
    var newCount: Int? = null,
    var reward: AttendanceRewardDto? = null
)

/**
 * 출석 보상. item(users.item 필드 키, 예: "heart")과 amount, 또는 gold 중 하나가 온다
 */
data class AttendanceRewardDto(
    var item: String? = null,
    var amount: Int? = null,
    var gold: Int? = null
)
