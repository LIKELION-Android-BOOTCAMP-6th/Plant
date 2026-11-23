package com.a32b.plant.presentation.studying.ui

import com.a32b.plant.core.util.TimeFormatter

/**
 * "같이 공부 중" 목록 한 줄의 공부 시간 문구 (닉네임 뒤에 붙으므로 앞에 공백)
 * 누적 시간이라 요약 규칙 "n시간 n분"을 따른다 (#649). 예: " 2시간 12분 째 공부중!"
 * 1분 미만(0 포함)은 "1분 미만 째"·"0분 째"가 어색해서 따로 " 방금 공부 시작!"으로 보여준다
 */
internal fun studyingUserTimeText(studyingTime: Long): String =
    if (studyingTime < ONE_MINUTE_MS) {
        " 방금 공부 시작!"
    } else {
        " ${TimeFormatter.formatToHoursMinutes(studyingTime)} 째 공부중!"
    }

private const val ONE_MINUTE_MS = 60_000L
