package com.a32b.plant.data.repository

import android.util.Log
import com.a32b.plant.data.model.StudyingUser
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StudyingRepository(private val db: FirebaseFirestore) {
    suspend fun getStudyingUser(tag: String): List<StudyingUser>{
        return try{
            db.collection("studying")
                .whereEqualTo("tag", tag)
                .get()
                .await()
                .toObjects(StudyingUser::class.java)
        } catch (e: Exception){
            Log.e("error", e.message.toString())
            emptyList()
        }
    }
    suspend fun updateStudyingUser(user: StudyingUser){
        try {
            val query = db.collection("studying")
                .whereEqualTo("uid", user.uid)
                .get()
                .await()

            if (query.isEmpty){
                db.collection("studying")
                    .add(user)
                    .await()
            }else{
                val docId = query.documents[0].id
                db.collection("studying")
                    .document(docId)
                    .update("studyingTime", user.studyingTime)
                    .await()
            }
        } catch (e: Exception){
            Log.e("error", e.message.toString())
        }

    }
    suspend fun deleteStudyingUser(){

    }

    suspend fun saveStudyLog(){

    }
}