package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class ClearStudyingSessionUseCase @Inject constructor(
    private val repository: StudyingRepository,
) {

    /**
     * 로컬 백업을 먼저 지운 뒤 "공부 중" 표시(studying/{uid}) 삭제
     * 서버 삭제가 실패하거나 거부돼도 로컬 백업은 지워지도록 순서를 둠
     * 비정상 종료(isInterrupted)라면 로컬 백업은 복구용으로 남기고 서버 정보만 지움
     */
    suspend operator fun invoke(isInterrupted: Boolean = false) : Result<Unit>{
        if (!isInterrupted) {
            val localResult = repository.clearLocalSession()
            if (localResult is Result.Failure) return localResult
        }
        return repository.deleteStudyingUserInfo()
    }
}
