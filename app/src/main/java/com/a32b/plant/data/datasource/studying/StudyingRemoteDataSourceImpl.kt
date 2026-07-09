package com.a32b.plant.data.datasource.studying

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyingRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
): StudyingRemoteDataSource {


}