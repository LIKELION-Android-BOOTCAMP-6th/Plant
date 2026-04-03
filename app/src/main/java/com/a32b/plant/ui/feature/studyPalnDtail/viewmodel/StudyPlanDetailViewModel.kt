package com.a32b.plant.ui.feature.studyPalnDtail.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NoOpNavigator
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.model.UserProfile
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.data.repository.UserRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.firestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
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

    // 이름 수정 다이얼로그 출력 여부
    private val _isEditDialogShown = MutableStateFlow(false)
    val isEditDialogShown = _isEditDialogShown.asStateFlow()

    //이름 수정 다이얼로그 표시/숨김 제어
    fun setEditDialogShown(show: Boolean){
        _isEditDialogShown.value = show
    }

    //화분 이름 업데이트
    fun updatePotName(newName: String){
        if (isInvalidIds(userId, potId) || newName.isBlank()) return

        db.collection("users").document(userId)
            .collection("pots").document(potId)
            .update("name", newName) // Firestore의 'name' 필드만 업데이트
            .addOnSuccessListener {
                fetchPotDetail() // UI 갱신을 위해 데이터 다시 불러오기
                setEditDialogShown(false) // 다이얼로그 닫기
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "이름 수정 실패: ${e.message}")
                //Toast.makeText("수정 실패", "")
            }
    }

    // 상세 기록 삭제 확인 다이얼로그 출력 여부
    private val _isDeleteDialogShown = MutableStateFlow(false)
    val isDeleteDialogShown = _isDeleteDialogShown.asStateFlow()

    //상세 기록 삭제 대기 로그 ID
    private var pendingDeleteLogID : String = ""

    fun showDeleteDialog(logId: String){
        pendingDeleteLogID = logId
        _isDeleteDialogShown.value = true
    }

    fun dismissDeleteDialog(){
        _isDeleteDialogShown.value = false
        pendingDeleteLogID = ""
    }

    //상세 기록 최종 삭제
    fun confirmDelete(){
        if(pendingDeleteLogID.isNotEmpty()){
            deleteStudyLog(pendingDeleteLogID)
            dismissDeleteDialog()
        }
    }

    // 화분 전체 삭제 다이얼로그 상태
    private val _isPotDeleteDialogShown = MutableStateFlow(false)
    val isPotDeleteDialogShown = _isPotDeleteDialogShown.asStateFlow()

    fun setPotDeleteDialogShown(show: Boolean){
        _isPotDeleteDialogShown.value = show
    }

    //화분 전체 삭제
    fun confirmDeleteEntirePot(onSuccess: () -> Unit){
        if(isInvalidIds(userId, potId)) return

        val timeToSubtract = _potDetail.value?.potTotalStudyingTime ?: 0L

        viewModelScope.launch {
            val userRef = db.collection("users").document(userId)
            val potRef = db.collection("users").document(userId)
                .collection("pots").document(potId)
            val batch = db.batch()
            // 유저 전체 공부 시간 차감
            batch.update(userRef, "totalStudyTime", com.google.firebase.firestore.FieldValue.increment(timeToSubtract * -1))
            // 화분 문서 삭제
            batch.delete(potRef)

            batch.commit()
                .addOnSuccessListener {
                    setPotDeleteDialogShown(false)
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "화분 삭제 실패: ${e.message}")
                }
        }
    }

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

        //삭제 로그 시간 찾기
        val logToDelete = _studyLogs.value.find { it.id == logId }
        val timeToSubtract = logToDelete?.studyingTime ?: 0L

        viewModelScope.launch {
            db.collection("users").document(userId)
                .collection("pots").document(potId)
                .collection("logs").document(logId)
                .delete()
                .addOnSuccessListener {
                    //총 공부시간에서 시간 차감
                    val decreaseAmount = timeToSubtract * -1

                    // 화분 총 시간, 전체 총 시간 동시 차감
                    updateGlobalAndPotTotalTime(decreaseAmount)

                    // 삭제 후 리스트 새로고침
                    fetchStudyLogs()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "삭제 실패 : ${e.message}")
                }
        }
    }
    private fun updateGlobalAndPotTotalTime(amount: Long) {
        if (isInvalidIds(userId, potId)) return

        val incrementValue = com.google.firebase.firestore.FieldValue.increment(amount)

        // 전체 총 공부시간
        val userRef = db.collection("users").document(userId)

        //화분 총 공부시간
        val potRef = db.collection("users").document(userId)
            .collection("pots").document(potId)

        // 하나의 트랜잭션으로 묶기
        val batch = db.batch()

        // 유저 업데이트
        batch.update(userRef, "totalStudyTime", incrementValue)

        // 화분 업데이트
        batch.update(potRef, "potTotalStudyingTime", incrementValue)

        batch.commit()
            .addOnSuccessListener {
                Log.d("Firestore", "총 시간 업데이트 성공: $amount")
                // UI 수치 갱신
                fetchPotDetail()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "총 시간 업데이트 실패: ${e.message}")
            }
    }
    fun checkID(logId: String): Boolean{
        return when {
            userId.isEmpty() || potId.isEmpty() || logId.isEmpty() -> true
            else -> false
        }
    }

    //화분 전체 삭제
    fun deleteEntriePot(onSuccess: () -> Unit){
        if(isInvalidIds(userId, potId)) return

        // 삭제 화분의 총 공부시간 저장
        val timeToSubtract = _potDetail.value?.potTotalStudyingTime ?: 0L

        viewModelScope.launch {
            // 유저
            val userRef = db.collection("users").document(userId)

            // 화분
            val potRef = db.collection("users").document(userId)
                .collection("pots").document(potId)

            val batch = db.batch()

            //시간 감소
            batch.update(userRef, "totalStudyTime", com.google.firebase.firestore.FieldValue.increment(timeToSubtract * -1))

            // 화분 삭제
            batch.delete(potRef)

            batch.commit()
                .addOnSuccessListener {
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore","화분 삭제 실패 : ${e.message}")
                    // Toast.make

                }


        }
    }

    //선택한 학습 로그
    private val _selectedStudyLog = MutableStateFlow<StudyLog?>(null)
    val selectedStudyLog = _selectedStudyLog.asStateFlow()

    //리스트 클릭할 때
    fun onStudyLogClicked(log: StudyLog){
        _selectedStudyLog.value = log
    }

    //리스트 다이얼로그 닫을 때
    fun onDismissLogDialog(){
        _selectedStudyLog.value = null
    }

    // 학습 완료
    //다이얼로그 상태
    private val _isCompleteDialogShown = MutableStateFlow(false)
    val isCompleteDialogShown = _isCompleteDialogShown.asStateFlow()

    fun setCompleteDialogShown(show: Boolean){
        _isCompleteDialogShown.value = show
    }

    //학습 완료 처리 -> DB 값 변경
    fun completeStudyPlan(onSuccess: () -> Unit){
        if(isInvalidIds(userId, potId)) return

        viewModelScope.launch {
            val userRef = db.collection("users").document(userId)
            val potRef = db.collection("users").document(userId)
                .collection("pots").document(potId)

            val batch = db.batch()

            // 완료 화분 수 증가
            batch.update(userRef, "completedPotsCount", com.google.firebase.firestore.FieldValue.increment(1))

            // 화분 상태 변경
            batch.update(potRef,
                mapOf(
                    "isCompleted" to true,
                    "completedAt" to FieldValue.serverTimestamp()
                )
            )

            batch.commit()
                .addOnSuccessListener {
                    setCompleteDialogShown(false)
                    onSuccess()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "학습 완료 처리 실패 : ${e.message}")
                }
        }
    }

    //공유 기능 체크박스
    //전체 선택 체크
    val isAllSelected: Boolean
        get() = _studyLogs.value.isNotEmpty() && _studyLogs.value.all { it.isSelected }

    //각 항목 체크박스 상태로 변경
    fun onLogSelectionChanged(logId: String, isSelected: Boolean){
        _studyLogs.value = _studyLogs.value.map {
            if(it.id == logId){
                it.copy(isSelected = isSelected)
            } else it
        }
    }

    //전체 선택/해제
    fun toggleAllSelection(selected: Boolean){
        _studyLogs.value = _studyLogs.value.map{it.copy(isSelected = selected)}
    }

    fun navigateToCommunityShare(navController: NavController){
        val selectedIds = _studyLogs.value.filter { it.isSelected }.map { it.id }
        val pot = _potDetail.value?: return

        if(selectedIds.isNotEmpty()){
            navController.navigate(
                Routes.CommunityPost(
                    potId = pot.id,
                    tagId = pot.tag_id,
                    title = pot.name,
                    studyLogIds = selectedIds
                )
            )
        }
    }
    //공유 모드 상태
    private val _isShareMode = MutableStateFlow(false)
    val isShareMode = _isShareMode.asStateFlow()

    //공유버튼 클릭
    fun handleShareAction(navController: NavController){
        if(!_isShareMode.value){
            _isShareMode.value = true
        } else{
            val selectedIds = _studyLogs.value.filter { it.isSelected }.map { it.id }
            val pot = _potDetail.value?: return

            if(selectedIds.isNotEmpty()){
                navController.navigate(
                    Routes.CommunityPost(
                        potId = pot.id,
                        tagId = pot.tag_id,
                        title = pot.name,
                        studyLogIds = selectedIds
                    )
                )
                //이동 후 공유모드 해제
                cancelShareMode()
            } else {
                _isShareMode.value = false
            }
        }
    }

    //공유 모드 취소 기능
    fun cancelShareMode(){
        _isShareMode.value = false
        toggleAllSelection(false)
    }

    //공유모드 상태 변경
    fun setShareMode(show: Boolean){
        _isShareMode.value = show
        if(!show){
            toggleAllSelection(false)
        }
    }
}