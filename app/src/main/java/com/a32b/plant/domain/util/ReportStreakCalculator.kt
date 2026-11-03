package com.a32b.plant.domain.util

import com.a32b.plant.domain.model.ReportDailyTotal

const val MIN_STUDYING_TIME_MILLIS = 60_000L
const val FIRST_STREAK_CHUNK_DAYS = 7L
const val NEXT_STREAK_CHUNK_DAYS = 30L

fun isStudyDay(totalMillis: Long): Boolean = totalMillis >= MIN_STUDYING_TIME_MILLIS

fun longestConsecutiveStudyDays(dailyTotals: List<ReportDailyTotal>): Int {
    var currentStreakDays = 0
    var longestStreakDays = 0

    dailyTotals.forEach { dailyTotal ->
        if (isStudyDay(dailyTotal.totalMillis)) {
            currentStreakDays++
            longestStreakDays = maxOf(longestStreakDays, currentStreakDays)
        } else {
            currentStreakDays = 0
        }
    }

    return longestStreakDays
}
