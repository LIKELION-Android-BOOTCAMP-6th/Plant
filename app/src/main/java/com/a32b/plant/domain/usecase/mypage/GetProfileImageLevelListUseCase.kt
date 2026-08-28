package com.a32b.plant.domain.usecase.mypage

import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.repository.PotRepository
import javax.inject.Inject

class GetProfileImageLevelListUseCase @Inject constructor(
    private val potRepository: PotRepository
) {
    suspend operator fun invoke(): List<String> {
        val levelList = potRepository.getDuplicationLevelList(CurrentUser.uid)
        return levelList.ifEmpty { listOf("") }
    }
}
