package com.a32b.plant.data.source.remote.community

import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity

interface CommunityRemoteDataSource {

    suspend fun addComment(postId : String, comment: Comment, activity: CommunityActivity)
}
