package com.a32b.plant.domain.usecase.studyLog

import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.StudyLogRepository
import javax.inject.Inject

class GetSelectedStudyLogUseCase @Inject constructor(
    private val studyLogRepository: StudyLogRepository
) {
    suspend operator fun invoke(uid: String, potId: String, logId: String): StudyLog? =
        studyLogRepository.getSelectedStudyLog(uid, potId, logId)
}