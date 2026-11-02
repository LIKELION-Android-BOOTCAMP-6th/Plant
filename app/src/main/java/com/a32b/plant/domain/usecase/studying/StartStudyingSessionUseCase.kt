package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class StartStudyingSessionUseCase @Inject constructor(
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val repository: StudyingRepository
) {
    /**
     * 로컬 백업을 먼저 저장한 뒤 "공부 중" 표시를 등록
     * 오프라인에서는 원격 등록이 연결될 때까지 끝나지 않으므로, 호출부는 이 함수의 완료를 기다리는 후속 작업을 두면 안 됨
     * 원격 등록 실패는 공부 진행에 영향이 없어 로컬 저장 결과만 반환 (에러 로그는 레포지토리에서 찍고 있음)
     */
    suspend operator fun invoke(tag: String, title: String, potId: String, time: Long, log: List<String>?, startedAt: Long) : Result<Unit> {
        return ensureCurrentUserUseCase {
            val localResult = repository.saveLocalSession(
                StudyingSession(it.uid, tag, title, potId, time, log, startedAt)
            )
            repository.initStudyingUser(
                StudyingUser(it.uid, it.nickname, it.profileImg, tag, time)
            )
            localResult
        } ?: Result.Failure(AppError.UnknownUser())
    }
}
