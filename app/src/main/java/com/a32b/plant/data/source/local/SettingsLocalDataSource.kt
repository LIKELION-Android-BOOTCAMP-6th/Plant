package com.a32b.plant.data.source.local

import kotlinx.coroutines.flow.Flow

interface SettingsLocalDataSource {
    val isDarkMode: Flow<Boolean?>

    suspend fun updateDarkMode(isDarkMode: Boolean)

    /** 학습 화면 알림 설정 안내를 마지막으로 띄운 날 (LocalDate.toEpochDay) — 한 번도 안 띄웠으면 null (#610) */
    suspend fun readNotificationGuideShownEpochDay(): Long?

    suspend fun saveNotificationGuideShownEpochDay(epochDay: Long)
}
