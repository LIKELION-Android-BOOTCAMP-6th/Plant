package com.a32b.plant.data.repository

import com.a32b.plant.data.datasource.studying.StudyingRemoteDataSource
import com.a32b.plant.data.datasource.user.UserRemoteDataSource
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource
): UserRepository {
}