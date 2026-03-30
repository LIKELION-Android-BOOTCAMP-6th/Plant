package com.a32b.plant.core.util

import com.google.firebase.Timestamp
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeFormatter {
    /**
    분단위 표시 : fun formatToMinute(time: Long): String
     -> ~분
     시분초 표시 : fun formatToDigitalClock(time: Long): String
     -> HH:mm:ss

     로컬 시간 날짜만 표시 : fun formatToKoreanDate(dateTime: LocalDateTime): String
     -> yyyy년 MM월 dd일

     로컬 시간 시간 표시 : fun formatToTimeOnly(dateTime: LocalDateTime): String

     파이어스토어 타임스탬프 -> yyyy년 MM월 dd일 : fun formatTimestamp(timestamop: Timestamp): String
     */
    fun formatToMinute(time: Long): String = "${time/1000/60}분"

    fun formatToDigitalClock(millis: Long): String{
        val totalSec = millis / 1000
        val hours = totalSec / 3600
        val minute = (totalSec % 3600) / 60
        val secs = totalSec % 60

        return if (hours > 0) String.format("%02d:%02d:%02d", hours, minute, secs)
        else String.format("%02d:%02d", minute, secs)
    }

    fun formatToKoreanDate(dateTime: LocalDateTime): String
        = dateTime.format(DateTimeFormatter.ofPattern("yyyy년 MM월 dd일"))

    fun formatToTimeOnly(dateTime: LocalDateTime): String
        = dateTime.format(DateTimeFormatter.ofPattern("HH:mm"))

    fun formatTimestamp(timestamp: Timestamp): String
        = SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA).format(timestamp.toDate())
}