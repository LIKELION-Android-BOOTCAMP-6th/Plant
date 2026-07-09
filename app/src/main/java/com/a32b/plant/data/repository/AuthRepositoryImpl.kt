package com.a32b.plant.data.repository

import com.a32b.plant.data.datasource.auth.AuthRemoteDataSource
import com.a32b.plant.domain.repository.AuthRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val authRemoteDataSource: AuthRemoteDataSource
): AuthRepository {
}