package com.a32b.plant.data.local

import com.a32b.plant.data.model.StudyingSession

interface StudyingLocalDataSource {
    suspend fun save(studying: StudyingSession)

    suspend fun read(): StudyingSession?

    suspend fun clear()
}