package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.StudyLog
import kotlinx.coroutines.flow.Flow

interface StudyLogRepository {
    fun getStudyLogs(uid: String, potId: String): Flow<List<StudyLog>>
    suspend fun getPotLogs(uid: String, potId: String): List<StudyLog>
    suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): StudyLog?
    fun createStudyLog(uid: String, potId: String, studyLog: StudyLog)
    suspend fun deleteStudyLog(uid: String, potId: String, logId: String, studyingTime: Long)
}