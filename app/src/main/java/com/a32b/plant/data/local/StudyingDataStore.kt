package com.a32b.plant.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private const val PREFERENCE_NAME = "studying_local"
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = PREFERENCE_NAME)
object StudyingDataStore {
    object Keys {
        val USER_ID = stringPreferencesKey("userId")
        val POT_ID = stringPreferencesKey("potId")
        val TAG = stringPreferencesKey("tag")
        val TITLE = stringPreferencesKey("title")
        val TIME = longPreferencesKey("time")
    }

    suspend fun save(context: Context, studying: StudyingSession){
        context.dataStore.edit { preferences ->
            preferences[Keys.USER_ID] = studying.userId!!
            preferences[Keys.POT_ID] = studying.potId!!
            preferences[Keys.TAG] = studying.tag!!
            preferences[Keys.TITLE] = studying.title!!
            preferences[Keys.TIME] = studying.time!!
        }
    }

    fun read(context: Context): Flow<StudyingSession>{
        return context.dataStore.data.map { preferences ->
            StudyingSession(
                userId = preferences[Keys.USER_ID],
                potId = preferences[Keys.POT_ID],
                tag = preferences[Keys.TAG],
                title = preferences[Keys.TITLE],
                time = preferences[Keys.TIME]
            )
        }
    }

    suspend fun clear(context: Context){
        context.dataStore.edit { it.clear() }
    }

}