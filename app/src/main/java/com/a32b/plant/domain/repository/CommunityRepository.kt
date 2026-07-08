package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.result.Result

interface CommunityRepository {

    suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity) : Result<Unit>
}