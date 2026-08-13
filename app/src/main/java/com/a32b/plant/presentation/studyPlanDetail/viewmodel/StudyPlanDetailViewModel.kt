package com.a32b.plant.presentation.studyPlanDetail.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.pot.*
import com.a32b.plant.domain.usecase.studyLog.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class StudyPlanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPotDetailUseCase: GetPotDetailUseCase,
    private val getStudyLogsUseCase: GetStudyLogsUseCase,
    private val updatePotNameUseCase: UpdatePotNameUseCase,
    private val deleteStudyLogUseCase: DeleteStudyLogUseCase,
    private val deleteEntirePotUseCase: DeleteEntirePotUseCase,
    private val completeStudyPlanUseCase: CompleteStudyPlanUseCase
) : ViewModel() {
    private val auth = Firebase.auth

    // Navigation에서 넘겨준 potId
    private val args = savedStateHandle.toRoute<Routes.StudyPlanDetail>()
    private val potId: String = args.potId
    private val userId: String = auth.currentUser?.uid ?: ""

    private val _potDetail = MutableStateFlow<Pot?>(null)
    val potDetail = _potDetail.asStateFlow()

    private val _studyLogs = MutableStateFlow<List<StudyLog>>(emptyList())
    val studyLogs = _studyLogs.asStateFlow()

    // 이름 수정 다이얼로그 출력 여부
    private val _isEditDialogShown = MutableStateFlow(false)
    val isEditDialogShown = _isEditDialogShown.asStateFlow()

    private val _isDeleteDialogShown = MutableStateFlow(false)
    val isDeleteDialogShown = _isDeleteDialogShown.asStateFlow()
    private var pendingDeleteLogID: String = ""

    private val _isPotDeleteDialogShown = MutableStateFlow(false)
    val isPotDeleteDialogShown = _isPotDeleteDialogShown.asStateFlow()

    private val _selectedStudyLog = MutableStateFlow<StudyLog?>(null)
    val selectedStudyLog = _selectedStudyLog.asStateFlow()

    private val _isCompleteDialogShown = MutableStateFlow(false)
    val isCompleteDialogShown = _isCompleteDialogShown.asStateFlow()

    private val _isShareMode = MutableStateFlow(false)
    val isShareMode = _isShareMode.asStateFlow()

    init {
        loadPotDetail()
        loadStudyLogs()
    }

    private fun loadPotDetail() {
        if (userId.isEmpty() || potId.isEmpty()) return
        viewModelScope.launch {
            safeRunCatching {
                getPotDetailUseCase(userId,potId)
            }.onSuccess { pot ->
                _potDetail.value = pot
            }
        }
    }

    private fun loadStudyLogs() {
        if (userId.isEmpty() || potId.isEmpty()) return
        viewModelScope.launch {
            safeRunCatching {
                getStudyLogsUseCase(userId, potId)
            }.onSuccess { flow ->
                // flow를 collect하여 실제 List<StudyLog>를 대입
                flow.collect { result ->
                    result.onSuccess { logs ->
                        _studyLogs.value = logs
                    }.onFailure { error ->
                        Log.e("StudyPlanDetailVM", "Failed to load study logs: ${error.message}")
                    }
                }
            }.onFailure { error ->
                Log.e("StudyPlanDetailVM", "Failed to collect study logs flow: ${error.message}")
            }
        }
    }

    fun setEditDialogShown(show: Boolean){
        _isEditDialogShown.value = show
    }

    fun updatePotName(newName: String){
        if ( userId.isEmpty() || potId.isEmpty()) return
        viewModelScope.launch {
            safeRunCatching {
                updatePotNameUseCase(userId, potId, newName)
            }.onSuccess {
                loadPotDetail()
                setEditDialogShown(false)
            }
        }
    }

    fun showDeleteDialog(logId: String){
        pendingDeleteLogID = logId
        _isDeleteDialogShown.value = true
    }

    fun dismissDeleteDialog() {
        _isDeleteDialogShown.value = false
        pendingDeleteLogID = ""
    }

    fun confirmDelete() {
        if (pendingDeleteLogID.isNotEmpty()){
            val logToDelete = _studyLogs.value.find { it.id == pendingDeleteLogID }
            val studyingTime = logToDelete?.studyingTime ?: 0L

            viewModelScope.launch {
                safeRunCatching {
                    deleteStudyLogUseCase(userId, potId, pendingDeleteLogID, studyingTime)
                }.onSuccess {
                    dismissDeleteDialog()
                    loadPotDetail()
                }
            }
        }
    }

    fun setPotDeleteDialogShown(show: Boolean){
        _isPotDeleteDialogShown.value = show
    }

    fun confirmDeleteEntirePot(onSuccess: () -> Unit){
        if (userId.isEmpty() || potId.isEmpty()) return
        val totalStudyingTime = _potDetail.value?.potTotalStudyingTime ?: 0L

        viewModelScope.launch {
            safeRunCatching {
                deleteEntirePotUseCase(userId, potId, totalStudyingTime)
            }.onSuccess {
                setPotDeleteDialogShown(false)
                onSuccess()
            }
        }
    }

    fun onStudyLogClicked(log: StudyLog){
        _selectedStudyLog.value = log
    }

    fun onDismissLogDialog() {
        _selectedStudyLog.value = null
    }

    fun setCompleteDialogShown(show: Boolean){
        _isCompleteDialogShown.value = show
    }

    fun completeStudyPlan(onSuccess: () -> Unit){
        if (userId.isEmpty() || potId.isEmpty()) return
        viewModelScope.launch {
            safeRunCatching {
                completeStudyPlanUseCase(userId, potId)
            }.onSuccess {
                setCompleteDialogShown(false)
                onSuccess()
            }
        }
    }

    val isAllSelected: Boolean
        get() = _studyLogs.value.isEmpty() && _studyLogs.value.all { it.isSelected }

    fun onLogSelectionChanged(logId: String, isSelected: Boolean){
        _studyLogs.value = _studyLogs.value.map {
            if (it.id == logId) it.copy(isSelected = isSelected) else it
        }
    }

    fun toggleAllSelection(selected: Boolean){
        _studyLogs.value = _studyLogs.value.map { it.copy(isSelected = selected) }
    }

    fun setShareMode(show: Boolean){
        _isShareMode.value = show
        if (!show) toggleAllSelection(false)
    }

    fun navigateToCommunityShare(navController: NavController){
        val selectedIds = _studyLogs.value.filter { it.isSelected }.map { it.id }
        val pot = _potDetail.value ?: return

        if (selectedIds.isNotEmpty()){
            navController.navigate(
                Routes.CommunityPost(
                    potId = pot.id,
                    tagId = pot.tagId,
                    title = pot.name,
                    studyLogIds = selectedIds
                )
            )
            setShareMode(false)
        }
    }
}