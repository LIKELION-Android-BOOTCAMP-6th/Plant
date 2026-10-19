package com.a32b.plant.data.source.local

import com.a32b.plant.data.model.StudyingSessionDto

interface StudyingLocalDataSource {
    suspend fun save(studying: StudyingSessionDto)

    suspend fun read(): StudyingSessionDto?

    suspend fun clear()
}
