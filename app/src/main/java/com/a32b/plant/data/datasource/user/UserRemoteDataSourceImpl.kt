package com.a32b.plant.data.datasource.user

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
): UserRemoteDataSource {


}