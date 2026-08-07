package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface StudyLogRepository {
    fun getStudyLogs(uid: String, potId: String): Flow<Result<List<StudyLog>>>
    suspend fun getPotLogs(uid: String, potId: String): Result<List<StudyLog>>
    suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): Result<StudyLog?>
    suspend fun deleteStudyLog(uid: String, potId: String, logId: String, studyingTime: Long): Result<Unit>
}