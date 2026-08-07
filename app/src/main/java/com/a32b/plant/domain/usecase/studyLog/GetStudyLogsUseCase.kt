package com.a32b.plant.domain.usecase.studyLog

import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.StudyLogRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetStudyLogsUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository
) {
    // 특정 화분의 학습 기록 목록을 실시간 Flow로 조회
    operator fun invoke(uid: String, potId: String): Flow<List<StudyLog>> =
        studyLogRepository.getStudyLogs(uid, potId)
}