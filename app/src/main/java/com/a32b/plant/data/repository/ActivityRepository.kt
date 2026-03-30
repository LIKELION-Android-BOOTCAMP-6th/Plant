package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.CommunityActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

class ActivityRepository(private val db: FirebaseFirestore){
    suspend fun getActivityList(selected: String): List<CommunityActivity>{
        Log.d("레퍼지토리", selected)
        return try {
            db.collection("activities")
                .whereEqualTo("uid", CurrentUser.uid)
                .whereEqualTo("type", selected)
                .get()
                .await()
                .toObjects(CommunityActivity::class.java)
                .sortedByDescending { it.createAt }
        } catch (e: Exception){
            Log.e("액티비티 레퍼지토리", e.message.toString())
            emptyList()
        }


    }

}