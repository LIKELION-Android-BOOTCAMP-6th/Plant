package com.a32b.plant.ui.feature.studyPalnDtail.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.model.UserProfile
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.data.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class StudyPlanDetailViewModel(
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val db = Firebase.firestore
    private val auth = Firebase.auth

    // Navigation에서 넘겨준 potId
    private val potId: String = checkNotNull(savedStateHandle["potId"])
    private val userId: String = auth.currentUser?.uid ?: ""

    private val _potDetail = MutableStateFlow<PotInfo?>(null)
    val potDetail = _potDetail.asStateFlow()

    private val _studyLogs = MutableStateFlow<List<StudyLog>>(emptyList())
    val studyLogs = _studyLogs.asStateFlow()

    init {
        fetchPotDetail()
    }

    private fun fetchPotDetail() {
        if (userId.isEmpty()) return

        // Firestore 경로: users/{userId}/pots/{potId}
        db.collection("users").document(userId)
            .collection("pots").document(potId)
            .collection("logs")
            .orderBy("createAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val logs = querySnapshot.toObjects(StudyLog::class.java)
                _studyLogs.value = logs
            }
            .addOnFailureListener { e ->
                Log.e("Firestore","데이터 로드 실패 : ${e.message}")
                //Toast.makeText(context, "")
            }
    }
}