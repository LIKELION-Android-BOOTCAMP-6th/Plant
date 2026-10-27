package com.a32b.plant.domain.util

import com.a32b.plant.domain.model.ReportDailyTotal

const val MIN_STUDYING_TIME_MILLIS = 60_000L

fun longestConsecutiveStudyDays(dailyTotals: List<ReportDailyTotal>): Int {
    var currentStreakDays = 0
    var longestStreakDays = 0

    dailyTotals.forEach { dailyTotal ->
        if (dailyTotal.totalMillis >= MIN_STUDYING_TIME_MILLIS) {
            currentStreakDays++
            longestStreakDays = maxOf(longestStreakDays, currentStreakDays)
        } else {
            currentStreakDays = 0
        }
    }

    return longestStreakDays
}
