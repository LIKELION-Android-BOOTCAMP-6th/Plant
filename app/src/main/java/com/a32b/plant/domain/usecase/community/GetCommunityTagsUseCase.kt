package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class GetCommunityTagsUseCase @Inject constructor(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(): Result<List<Tag>> = repository.getTags()
}
