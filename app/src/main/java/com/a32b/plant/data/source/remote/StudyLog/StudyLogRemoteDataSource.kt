package com.a32b.plant.data.datasource

import com.a32b.plant.data.model.StudyLogDto
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import kotlinx.coroutines.flow.Flow

interface StudyLogRemoteDataSource {
    fun getStudyLogs(uid: String, potId: String): Flow<List<StudyLogDto>>
    suspend fun getPotLogs(uid: String, potId: String): List<StudyLogDto>
    suspend fun getSelectedStudyLog(uid: String, potId: String, logId: String): StudyLogDto?

    /** 기기 캐시의 기록 (캐시에 없으면 기본 조회). 없는 기록이면 null (#455) */
    suspend fun getStudyLogPreferringCache(uid: String, potId: String, logId: String): StudyLogDto?

    /** 서버에 기록이 있는지 — 삭제가 거부된 뒤 이미 지워졌는지 확인할 때 사용 (#455) */
    suspend fun existsStudyLogOnServer(uid: String, potId: String, logId: String): Boolean

    /**
     * 학습 기록 삭제 batch를 시작하고 서버 반영 Task를 돌려준다 (#455, firestore.rules potStudyLogDeleted·userStudyLogDeleted).
     * 기록 삭제 + 화분·유저 학습 시간 [studyingTime]만큼 차감. [studyingTime]은 저장된 기록의 값이어야 규칙을 통과한다
     */
    fun deleteStudyLog(uid: String, potId: String, logId: String, studyingTime: Long): Task<Void>
    suspend fun getPotLogsInPeriod(
        uid: String,
        potId: String,
        startInclusive: Timestamp,
        endExclusive: Timestamp
    ): List<StudyLogDto>
}