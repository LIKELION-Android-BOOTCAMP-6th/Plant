package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class ClearStudyingSessionUseCase @Inject constructor(
    private val repository: StudyingRepository,
) {

    suspend operator fun invoke(isInterrupted: Boolean = false) : Result<Unit>{

        val result = repository.deleteStudyingUserInfo()

        //네트워크 오류가 아닐 경우 원격 실행 결과 리턴
        if (result is Result.Failure && result.error !is AppError.Network) return result

        //비정상 종료라면 studying 컬랙션에서 유저 정보만 지우고, 그게 아니라면 로컬 정보까지 지운 뒤 리턴
        return if (!isInterrupted) repository.clearLocalSession() else result
    }
}
