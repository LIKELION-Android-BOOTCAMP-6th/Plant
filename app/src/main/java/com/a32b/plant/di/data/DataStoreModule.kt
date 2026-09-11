package com.a32b.plant.di.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataStoreModule {

    // 공부 세션을 지워도 기기 설정은 유지되도록 별도 파일에 저장한다.
    @Provides
    @Singleton
    fun provideSettingsDataStore(
        @ApplicationContext context: Context
    ): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        // 파일이 손상되면 저장 전 상태(라이트 모드)로 되돌려 읽기 실패가 반복되지 않게 한다.
        corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
        produceFile = { context.preferencesDataStoreFile("settings_local") }
    )
}
