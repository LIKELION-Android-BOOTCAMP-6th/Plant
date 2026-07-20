package com.a32b.plant.data.repository

import com.a32b.plant.data.datasource.pot.PotRemoteDataSource
import com.a32b.plant.data.datasource.studying.StudyingRemoteDataSource
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.repository.StudyingRepository
import javax.inject.Inject
import javax.inject.Singleton


@Singleton
class StudyingRepositoryImpl @Inject constructor(
    private val studyingRemoteDataSource: StudyingRemoteDataSource
): StudyingRepository {
}