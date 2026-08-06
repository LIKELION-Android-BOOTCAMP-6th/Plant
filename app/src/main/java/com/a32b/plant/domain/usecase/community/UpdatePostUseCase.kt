package com.a32b.plant.domain.usecase.community

import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

class UpdatePostUseCase @Inject constructor(
    private val repository: CommunityRepository
) {
    suspend operator fun invoke(isShared: Boolean, postId: String, title: String, content: String?, tag: Tag?): Result<Unit> =
        repository.updatePost(isShared, postId, title, content, tag)
}
