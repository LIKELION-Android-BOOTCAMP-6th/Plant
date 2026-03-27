package com.a32b.plant.ui.feature.studyPalnDtail.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
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
    private val args = savedStateHandle.toRoute<Routes.StudyPlanDetail>()
    private val potId: String = args.potId
    private val userId: String = auth.currentUser?.uid ?: ""

    private val _potDetail = MutableStateFlow<PotInfo?>(null)
    val potDetail = _potDetail.asStateFlow()

    private val _studyLogs = MutableStateFlow<List<StudyLog>>(emptyList())
    val studyLogs = _studyLogs.asStateFlow()

    init {
        fetchPotDetail()
        fetchStudyLogs()
    }

    //ID 검사
    private fun isInvalidIds(vararg ids : String?): Boolean{
        return ids.any {it.isNullOrEmpty()}
    }

    //화분 상세 정보 가져오기
    private fun fetchPotDetail() {
        if (isInvalidIds(userId, potId)) return

        // Firestore 경로: users/{userId}/pots/{potId}
        db.collection("users").document(userId)
            .collection("pots").document(potId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    _potDetail.value = document.toObject(PotInfo::class.java)?.copy(id = document.id)
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore","데이터 로드 실패 : ${e.message}")
                //Toast.makeText(context, "")
            }
    }
    private fun fetchStudyLogs(){
        if(isInvalidIds(userId, potId)) return
        db.collection("users").document(userId)
            .collection("pots").document(potId)
            .collection("logs")
            .orderBy("createAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener { querySnapshots ->
                val logs = querySnapshots.documents.mapNotNull { doc ->
                    doc.toObject(StudyLog::class.java)?.copy(id = doc.id)
                }
                _studyLogs.value = logs
            }
    }
    fun deleteStudyLog(logId: String){
        if(isInvalidIds(userId, potId, logId)) return
        viewModelScope.launch {
            db.collection("users").document(userId)
                .collection("pots").document(potId)
                .collection("logs").document(logId)
                .delete()
                .addOnSuccessListener {
                    // 삭제 후 리스트 새로고침
                    fetchStudyLogs()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "삭제 실패 : ${e.message}")
                }
        }
    }
    fun checkID(logId: String): Boolean{
        return when {
            userId.isEmpty() || potId.isEmpty() || logId.isEmpty() -> true
            else -> false
        }
    }
}