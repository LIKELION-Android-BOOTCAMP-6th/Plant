package com.a32b.plant.data.source.local

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
import kotlinx.serialization.json.Json
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
    private val LOG = stringPreferencesKey("log")

    override suspend fun save(studying: StudyingSession) {
        context.dataStore.edit { preferences ->
            preferences[USER_ID] = studying.userId
            preferences[POT_ID] = studying.potId
            preferences[TAG] = studying.tag
            preferences[TITLE] = studying.title
            preferences[TIME] = studying.time
            studying.log?.let {
                preferences[LOG] = Json.encodeToString(it)
            }
        }
    }

    override suspend fun read(): StudyingSession? {
        val preferences = context.dataStore.data
            .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
            .first()

        val userId = preferences[USER_ID] ?: return null
        val potId = preferences[POT_ID] ?: return null
        val tag = preferences[TAG] ?: return null
        val title = preferences[TITLE] ?: return null
        val time = preferences[TIME] ?: return null
        val log = preferences[LOG]?.let {
            runCatching { Json.decodeFromString<List<String>>(it) }.getOrNull()
        } //log를 나중에 작성한 경우일 수 있으니 nullable

        return StudyingSession(
            userId = userId,
            potId = potId,
            tag = tag,
            title = title,
            time = time,
            log = log
        )
    }

    override suspend fun clear() {
        context.dataStore.edit { it.clear() }
    }
}
