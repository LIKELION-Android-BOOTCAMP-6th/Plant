package com.a32b.plant.data.repository

import android.content.Context
import android.util.Log
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.local.StudyingDataStore
import com.a32b.plant.data.local.StudyingSession
import com.a32b.plant.data.model.StudyingUser
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class StudyingRepository(private val db: FirebaseFirestore, private val appContext: Context) {
    private val datasore = StudyingDataStore
    suspend fun getStudyingUser(tag: String): List<StudyingUser>{
        return try{
            db.collection("studying")
                .whereEqualTo("tag", tag)
                .get()
                .await()
                .toObjects(StudyingUser::class.java)
                .sortedByDescending { it.studyingTime }
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

    fun deleteStudyingUser(){
        db.collection("studying")
            .whereEqualTo("uid", CurrentUser.uid)
            .get()
            .addOnSuccessListener { doc ->
                doc.firstOrNull()?.reference?.delete()
            }
    }
    fun updateUserTotalStudyTime(studyTime: Long) {
        db.collection("users").document(CurrentUser.uid)
            .update("totalStudyTime", FieldValue.increment(studyTime))
    }

    suspend fun saveSession(session: StudyingSession) = datasore.save(appContext, session)
    fun readSession() = datasore.read(appContext)
    suspend fun clearSession() = datasore.clear(appContext)

}