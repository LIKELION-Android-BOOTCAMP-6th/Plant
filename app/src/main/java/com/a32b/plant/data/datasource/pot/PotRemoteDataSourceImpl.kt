package com.a32b.plant.data.datasource.pot

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PotRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
): PotRemoteDataSource {


}