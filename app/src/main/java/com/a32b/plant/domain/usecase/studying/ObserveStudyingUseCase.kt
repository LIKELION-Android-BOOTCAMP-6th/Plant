package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.domain.repository.AuthRepository
import com.a32b.plant.domain.repository.StudyingRepository
import javax.inject.Inject


class ObserveStudyingUseCase @Inject constructor(
    private val authRepository: AuthRepository,
    private val studyingRepository: StudyingRepository
) {

}