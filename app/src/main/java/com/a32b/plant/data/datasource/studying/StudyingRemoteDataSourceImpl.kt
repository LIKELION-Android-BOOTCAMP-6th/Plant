package com.a32b.plant.data.datasource.studying

import android.content.Context
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.data.model.StudyingUserDto
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StudyingRemoteDataSourceImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
): StudyingRemoteDataSource {
    private val uid: String
        get() = auth.currentUser?.uid ?: ""
    override fun observeStudyingUser(tag: String): Flow<List<StudyingUserDto>> = callbackFlow {
        val listener = db.collection("studying")
            .whereEqualTo("tag", tag)
            .orderBy("studyingTime", Query.Direction.DESCENDING)
            .limit(3)
            .addSnapshotListener { snapshots, exception ->
                if (exception != null){
                    close(exception)
                    return@addSnapshotListener
                }
                val users = snapshots?.documents?.mapNotNull { doc ->
                    runCatching { doc.toObject(StudyingUserDto::class.java) }.getOrNull()
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove()}
    }

    override suspend fun initStudyingUser(user: StudyingUserDto){
        db.collection("studying")
            .document(user.uid)
            .set(user)
            .await()
    }

    override suspend fun updateStudyingTime(tag: String, time: Long){
        db.collection("studying")
            .document(uid)
            .update(
                mapOf(
                    "studyingTime" to time,
                    "tag" to tag
                )
            )
            .await()
    }

    override fun saveStudyLog(potId: String, log: StudyLogDto){
        db.collection("users")
            .document(uid)
            .collection("pots")
            .document(potId)
            .collection("logs")
            .add(log)
    }

    override fun deleteStudyingUserInfo(){
        db.collection("studying")
            .document(uid)
            .delete()
    }

    override fun updateTotalStudyTime(potId: String,time: Long) {
        db.collection("users")
            .document(uid)
            .collection("pots")
            .document(potId)
            .update("potTotalStudyingTime", FieldValue.increment(time))
    }


}