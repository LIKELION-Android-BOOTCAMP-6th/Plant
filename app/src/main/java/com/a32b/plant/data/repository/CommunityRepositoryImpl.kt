package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.datasource.community.CommunityRemoteDataSource
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Comment
import com.a32b.plant.domain.model.CommunityActivity
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val communityRemoteDataSource: CommunityRemoteDataSource
): CommunityRepository {

    override suspend fun addComment(postId: String, comment: Comment, activity: CommunityActivity): Result<Unit> {
        return try {
            communityRemoteDataSource.addComment(postId, comment, activity)
            Result.Success(Unit)
        } catch (e: Exception){
            e.printStackTrace()
            Log.e("Comment Error", e.message.toString())
            Result.Failure(AppError.Custom("등록에 실패했습니다."))
        }
    }
}