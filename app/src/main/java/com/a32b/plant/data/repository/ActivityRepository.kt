package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.CommunityActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class ActivityRepository(private val db: FirebaseFirestore){
    fun getActivityList(selected: String): Flow<List<CommunityActivity>> = callbackFlow {
        Log.d("uid", "${CurrentUser.uid} + $selected")
        val listener = db.collection("activities")
            .whereEqualTo("uid", CurrentUser.uid)
            .whereEqualTo("type", selected)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e("액티비티 레퍼지토리", error.message.toString())
                    close(error) // Flow 종료
                    return@addSnapshotListener
                }

                val list = snapshot
                    ?.toObjects(CommunityActivity::class.java)
                    ?.sortedByDescending { it.createAt }
                    ?: emptyList()

                trySend(list)
            }

        awaitClose { listener.remove() }
    }

}