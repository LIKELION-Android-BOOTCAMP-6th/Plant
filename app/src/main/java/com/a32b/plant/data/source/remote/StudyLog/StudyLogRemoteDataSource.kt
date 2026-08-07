package com.a32b.plant.data.datasource

import com.a32b.plant.data.model.StudyLogDto
import kotlinx.coroutines.flow.Flow

interface StudyLogRemoteDataSource {
    fun getStudyLogs(uid: String, potId: String): Flow<List<StudyLogDto>>
    suspend fun getPotLogs(uid: String, potId: String): List<StudyLogDto>
    suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): StudyLogDto?
    suspend fun executeDeleteBatch(uid: String, potId: String, logId: String, decreaseAmount: Long)
}