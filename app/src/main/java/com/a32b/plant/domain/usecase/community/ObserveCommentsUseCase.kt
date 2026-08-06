package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObserveCommentsUseCase @Inject constructor(
    private val repository: CommunityRepository
) {
    operator fun invoke(postId: String): Flow<List<Comment>> = repository.observeComments(postId)
}
