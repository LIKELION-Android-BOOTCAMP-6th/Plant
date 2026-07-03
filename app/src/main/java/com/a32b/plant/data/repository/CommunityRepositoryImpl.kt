package com.a32b.plant.data.repository

import com.a32b.plant.data.datasource.community.CommunityRemoteDataSource
import com.a32b.plant.domain.repository.CommunityRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRepositoryImpl @Inject constructor(
    private val communityRemoteDataSource: CommunityRemoteDataSource
): CommunityRepository {
}