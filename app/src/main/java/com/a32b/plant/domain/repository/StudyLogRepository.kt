package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.result.Result
import kotlinx.coroutines.flow.Flow

interface StudyLogRepository {
    fun getStudyLogs(uid: String, potId: String): Flow<Result<List<StudyLog>>>
    suspend fun getPotLogs(uid: String, potId: String): Result<List<StudyLog>>
    suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): Result<StudyLog?>

    /**
     * 학습 기록 삭제 (#455). 화분·유저 학습 시간 차감량은 저장된 기록 값으로 정한다.
     * 이미 없는 기록이면(연타·다른 기기에서 삭제) 성공
     */
    suspend fun deleteStudyLog(uid: String, potId: String, logId: String): Result<SaveResult>
}