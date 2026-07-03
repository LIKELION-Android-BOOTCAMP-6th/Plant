package com.a32b.plant.data.datasource.community

import com.google.firebase.firestore.FirebaseFirestore
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CommunityRemoteDataSourceImpl @Inject constructor(
    private val db: FirebaseFirestore
): CommunityRemoteDataSource {


}