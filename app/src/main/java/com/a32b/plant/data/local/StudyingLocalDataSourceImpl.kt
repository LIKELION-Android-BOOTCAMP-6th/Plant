package com.a32b.plant.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.a32b.plant.data.model.StudyingSession
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private const val PREFERENCE_NAME = "studying_local"
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)

@Singleton
class StudyingLocalDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : StudyingLocalDataSource {

    private val USER_ID = stringPreferencesKey("userId")
    private val POT_ID = stringPreferencesKey("potId")
    private val TAG = stringPreferencesKey("tag")
    private val TITLE = stringPreferencesKey("title")
    private val TIME = longPreferencesKey("time")

    override suspend fun save(studying: StudyingSession) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = studying.userId!!
            preferences[POT_ID] = studying.potId!!
            preferences[TAG] = studying.tag!!
            preferences[TITLE] = studying.title!!
            preferences[TIME] = studying.time!!
        }
    }

    override suspend fun read(): StudyingSession? {
        val preferences = context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()

        val userId = preferences[USER_ID] ?: return null

        return StudyingSession(
            userId = userId,
            potId = preferences[POT_ID],
            tag = preferences[TAG],
            title = preferences[TITLE],
            time = preferences[TIME]
        )
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}