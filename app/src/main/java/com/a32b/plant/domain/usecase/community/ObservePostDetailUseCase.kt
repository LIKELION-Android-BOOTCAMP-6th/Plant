package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.model.Post
import com.a32b.plant.domain.repository.CommunityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ObservePostDetailUseCase @Inject constructor(
    private val repository: CommunityRepository
) {
    operator fun invoke(postId: String): Flow<Post?> = repository.getPostDetail(postId)
}
