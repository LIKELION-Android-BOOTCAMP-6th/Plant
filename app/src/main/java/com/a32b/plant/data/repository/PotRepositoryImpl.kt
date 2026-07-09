package com.a32b.plant.data.repository

import com.a32b.plant.data.datasource.community.CommunityRemoteDataSource
import com.a32b.plant.data.datasource.pot.PotRemoteDataSource
import com.a32b.plant.domain.repository.CommunityRepository
import com.a32b.plant.domain.repository.PotRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PotRepositoryImpl @Inject constructor(
    private val potRemoteDataSource: PotRemoteDataSource
): PotRepository {
}