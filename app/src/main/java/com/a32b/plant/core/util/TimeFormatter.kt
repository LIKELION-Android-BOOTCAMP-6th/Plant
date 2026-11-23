package com.a32b.plant.core.util

import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

object TimeFormatter {
    /**
     흘러가는 시간 표시 : fun formatToDigitalClock(millis: Long): String
     -> HH:mm:ss 고정 (1시간 미만도 시 자리 포함, 예: 00:47:12)
     누적·요약 시간 표시 : fun formatToHoursMinutes(millis: Long): String
     -> n시간 n분 (초는 버림, 예: 2시간 25분·38시간 25분·5시간·47분)
        0 이하는 "0분", 0 초과 1분 미만(1ms~59,999ms)은 "1분 미만" (#649)

     로컬 시간 날짜만 표시 : fun formatToKoreanDate(dateTime: LocalDateTime): String
     -> yyyy년 MM월 dd일

     로컬 시간 시간 표시 : fun formatToTimeOnly(dateTime: LocalDateTime): String

     파이어스토어 타임스탬프 -> yyyy년 MM월 dd일 : fun formatTimestamp(timestamop: Timestamp): String
     */
    fun formatToDigitalClock(millis: Long): String{
        val totalSec = millis / 1000
        val hours = totalSec / 3600
        val minute = (totalSec % 3600) / 60
        val secs = totalSec % 60

        return String.format("%02d:%02d:%02d", hours, minute, secs)
    }

    fun formatToHoursMinutes(millis: Long): String {
        if (millis <= 0L) return "0분"
        val totalMinutes = millis / MINUTE_MS
        if (totalMinutes == 0L) return "1분 미만"
        val hours = totalMinutes / 60
        val minutes = totalMinutes % 60
        return when {
            hours > 0 && minutes > 0 -> "${hours}시간 ${minutes}분"
            hours > 0 -> "${hours}시간"
            else -> "${minutes}분"
        }
    }

    private const val MINUTE_MS = 60_000L

    fun formatToDateOnly(dateTime: LocalDateTime): String
        = dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd"))

    fun formatToKoreanDate(dateTime: LocalDateTime): String
        = dateTime.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))

    fun formatWithDayOfWeek(dateTime: LocalDateTime): String
            = dateTime.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일 E요일", Locale.KOREAN))

    fun formatToTimeOnly(dateTime: LocalDateTime): String
        = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))
    fun formatToTimeOnly(time: Long) : String = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(time))

    fun formatTimeToDate(time: Long): String{
        if (time == 0L) return "-" // 또는 "미설정" 등
        return SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(Date(time))
    }

    fun formatTimeWithClock(time: Long): String
        = SimpleDateFormat("yyyy년 MM월 dd일 HH:mm", Locale.KOREA).format(Date(time))

    fun formatTimeAgo(timestamp: Long): String {
        val now = System.currentTimeMillis()
        val diff = now - timestamp

        val seconds = diff / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            seconds < 60 -> "방금 전"
            minutes < 60 -> "${minutes}분 전"
            hours < 24 -> "${hours}시간 전"
            days < 7 -> "${days}일 전"
            else -> formatTimeToDate(timestamp)
        }
    }
}
