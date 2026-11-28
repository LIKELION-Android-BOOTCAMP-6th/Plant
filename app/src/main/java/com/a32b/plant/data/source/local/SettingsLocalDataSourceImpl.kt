package com.a32b.plant.data.source.local

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsLocalDataSourceImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : SettingsLocalDataSource {

    private val DARK_MODE = booleanPreferencesKey("is_dark_mode")
    private val NOTIFICATION_GUIDE_SHOWN_EPOCH_DAY = longPreferencesKey("notification_guide_shown_epoch_day")
    private val POT_SORT_TYPE = stringPreferencesKey("pot_sort_type")

    override val isDarkMode: Flow<Boolean?> = dataStore.data
        .map { preferences -> preferences[DARK_MODE] }

    override suspend fun updateDarkMode(isDarkMode: Boolean) {
        dataStore.edit { preferences ->
            preferences[DARK_MODE] = isDarkMode
        }
    }

    override suspend fun readNotificationGuideShownEpochDay(): Long? =
        dataStore.data.first()[NOTIFICATION_GUIDE_SHOWN_EPOCH_DAY]

    override suspend fun saveNotificationGuideShownEpochDay(epochDay: Long) {
        dataStore.edit { preferences ->
            preferences[NOTIFICATION_GUIDE_SHOWN_EPOCH_DAY] = epochDay
        }
    }

    override suspend fun readPotSortType(): String? =
        dataStore.data.first()[POT_SORT_TYPE]

    override suspend fun savePotSortType(sortTypeName: String) {
        dataStore.edit { preferences ->
            preferences[POT_SORT_TYPE] = sortTypeName
        }
    }
}
