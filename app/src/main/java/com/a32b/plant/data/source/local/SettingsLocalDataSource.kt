package com.a32b.plant.data.source.local

import kotlinx.coroutines.flow.Flow

interface SettingsLocalDataSource {
    val isDarkMode: Flow<Boolean?>

    suspend fun updateDarkMode(isDarkMode: Boolean)
}
