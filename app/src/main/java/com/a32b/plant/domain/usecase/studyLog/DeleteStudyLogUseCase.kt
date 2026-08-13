package com.a32b.plant.domain.usecase.studyLog

import com.a32b.plant.domain.repository.StudyLogRepository
import javax.inject.Inject

class DeleteStudyLogUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository
) {
    // 특정 학습 기록 삭제 및 연관된 공부 시간 차감
    suspend operator fun invoke(uid: String, potId: String, logId: String, studyingTime: Long) {
        studyLogRepository.deleteStudyLog(uid, potId, logId, studyingTime)
    }
}