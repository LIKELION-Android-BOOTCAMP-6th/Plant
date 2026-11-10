package com.a32b.plant.presentation.studyPlanDetail.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import androidx.navigation.NavController
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.pot.*
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.usecase.studyLog.*
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

sealed class StudyPlanDetailEvent {
    data class ShowToast(val message: String) : StudyPlanDetailEvent()
}

@HiltViewModel
class StudyPlanDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getPotDetailUseCase: GetPotDetailUseCase,
    private val getStudyLogsUseCase: GetStudyLogsUseCase,
    private val updatePotNameUseCase: UpdatePotNameUseCase,
    private val deleteStudyLogUseCase: DeleteStudyLogUseCase,
    private val deleteEntirePotUseCase: DeleteEntirePotUseCase,
    private val completeStudyPlanUseCase: CompleteStudyPlanUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) : BaseViewModel() {
    private val auth = Firebase.auth

    // Navigation에서 넘겨준 potId
    private val args = savedStateHandle.toRoute<Routes.StudyPlanDetail>()
    private val potId: String = args.potId
    private var userId: String = ""

    private val _potDetail = MutableStateFlow<Pot?>(null)
    val potDetail = _potDetail.asStateFlow()

    private val _studyLogs = MutableStateFlow<List<StudyLog>>(emptyList())
    val studyLogs = _studyLogs.asStateFlow()

    // 이름 수정 다이얼로그 출력 여부
    private val _isEditDialogShown = MutableStateFlow(false)
    val isEditDialogShown = _isEditDialogShown.asStateFlow()

    // 이름 저장 중 여부 — 저장 중에는 다시 보내지 않고 다이얼로그에 로딩 표시 (#563)
    private val _isRenaming = MutableStateFlow(false)
    val isRenaming = _isRenaming.asStateFlow()

    private val _event = Channel<StudyPlanDetailEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

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

    // 기록 삭제·화분 삭제·완료 중 여부 — 진행 중에는 다시 보내지 않고, 다이얼로그를 닫지 않으며 로딩 표시 (#455)
    private val _isWriting = MutableStateFlow(false)
    val isWriting = _isWriting.asStateFlow()

    init {
        checkUseAndLoadData()
    }

    private fun checkUseAndLoadData(){
        viewModelScope.launch {
            ensureCurrentUserUseCase { user ->
                userId = user.uid
                if(userId.isNotEmpty() && potId.isNotEmpty()){
                    // 화분 상세 + 학습 기록 첫 결과가 모두 도착할 때까지 LoadableScreen 유지
                    val studyLogsFirstLoaded = CompletableDeferred<Unit>()
                    val potDetailJob = loadPotDetail()
                    loadStudyLogs(studyLogsFirstLoaded)
                    potDetailJob?.join()
                    studyLogsFirstLoaded.await()
                }
            }
            loaded() // BaseViewModel: 성공/실패 모두 최초 시도 후 LoadableScreen 해제
        }
    }
    private fun loadPotDetail(): Job? {
        if (userId.isEmpty() || potId.isEmpty()){
            Log.e("StudyPlanDetailVM", "ERROR: userId or potId is empty!")
            return null
        }
        return viewModelScope.launch {
            safeRunCatching {
                getPotDetailUseCase(userId,potId)
            }.onSuccess { pot ->
                _potDetail.value = pot
            }.onFailure { error ->
                Log.e("StudyPlanDetailVM", "Failed to load pot detail: ${error.message}", error)
            }
        }
    }

    /** [firstLoaded]: 첫 결과(성공/실패) 수신 또는 방출 없이 종료 시 complete — 초기 로딩 해제 대기용 */
    private fun loadStudyLogs(firstLoaded: CompletableDeferred<Unit>? = null) {
        if (userId.isEmpty() || potId.isEmpty()) {
            firstLoaded?.complete(Unit)
            return
        }
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
                    firstLoaded?.complete(Unit)
                }
            }.onFailure { error ->
                Log.e("StudyPlanDetailVM", "Failed to collect study logs flow: ${error.message}")
            }
            // 방출 없이 Flow가 끝나거나 실패한 경우에도 대기 해제
            firstLoaded?.complete(Unit)
        }
    }

    fun setEditDialogShown(show: Boolean){
        // 저장 중에는 닫지 않는다 (결과에 따라 닫거나 유지)
        if (!show && _isRenaming.value) return
        _isEditDialogShown.value = show
    }

    fun updatePotName(newName: String){
        val pot = _potDetail.value ?: return
        if (potId.isEmpty() || _isRenaming.value) return
        _isRenaming.value = true

        viewModelScope.launch {
            try {
                updatePotNameUseCase(potId, pot.name, newName)
                    .onSuccess { result ->
                        // 오프라인(Pending)이어도 기기 캐시에는 이미 반영됐으므로 다시 불러오면 새 이름이 보인다
                        loadPotDetail()
                        _isRenaming.value = false
                        setEditDialogShown(false)
                        if (result == SaveResult.Pending) {
                            _event.send(StudyPlanDetailEvent.ShowToast(NETWORK_SLOW_MESSAGE))
                        }
                    }
                    .onFailure { error ->
                        // 세션 만료는 UseCase에서 처리(세션 만료 안내)하므로 토스트를 띄우지 않는다
                        if (error !is AppError.UnknownUser) {
                            _event.send(StudyPlanDetailEvent.ShowToast(error.message))
                        }
                    }
            } finally {
                _isRenaming.value = false
            }
        }
    }

    fun showDeleteDialog(logId: String){
        pendingDeleteLogID = logId
        _isDeleteDialogShown.value = true
    }

    fun dismissDeleteDialog() {
        // 삭제 중에는 닫지 않는다 (결과에 따라 닫거나 유지)
        if (_isWriting.value) return
        _isDeleteDialogShown.value = false
        pendingDeleteLogID = ""
    }

    // 차감할 학습 시간은 화면 값이 아니라 저장된 기록에서 읽는다 (Repository, #455)
    fun confirmDelete() {
        val logId = pendingDeleteLogID
        if (logId.isEmpty() || potId.isEmpty()) return

        runWrite(
            write = { deleteStudyLogUseCase(potId, logId) },
            onSaved = { result ->
                // 오프라인(Pending)이어도 기기 캐시에는 이미 반영됐으므로 다시 불러오면 줄어든 시간이 보인다
                dismissDeleteDialog()
                loadPotDetail()
                if (result == SaveResult.Pending) {
                    _event.send(StudyPlanDetailEvent.ShowToast(NETWORK_SLOW_MESSAGE))
                }
            }
        )
    }

    fun setPotDeleteDialogShown(show: Boolean){
        // 삭제 중에는 닫지 않는다 (결과에 따라 닫거나 유지)
        if (!show && _isWriting.value) return
        _isPotDeleteDialogShown.value = show
    }

    /**
     * 화분 삭제. 차감할 학습 시간은 화면 값이 아니라 저장된 화분에서 읽는다 (Repository, #455).
     * [onDeleted]: 화면을 닫기 전에 보여줄 안내(오프라인이면 [NETWORK_SLOW_MESSAGE], 아니면 null).
     * 화면을 닫으면 이벤트를 받을 수 없으므로 이벤트 대신 콜백으로 넘긴다
     */
    fun confirmDeleteEntirePot(onDeleted: (notice: String?) -> Unit){
        if (potId.isEmpty()) return

        runWrite(
            write = { deleteEntirePotUseCase(potId) },
            onSaved = { result ->
                setPotDeleteDialogShown(false)
                onDeleted(result.noticeOrNull())
            }
        )
    }

    fun onStudyLogClicked(log: StudyLog){
        _selectedStudyLog.value = log
    }

    fun onDismissLogDialog() {
        _selectedStudyLog.value = null
    }

    fun setCompleteDialogShown(show: Boolean){
        // 완료 처리 중에는 닫지 않는다 (결과에 따라 닫거나 유지)
        if (!show && _isWriting.value) return
        _isCompleteDialogShown.value = show
    }

    /**
     * 화분 완료 (#455). 이미 완료된 화분이면 다시 보내지 않고 성공 처리된다 (UseCase·Repository).
     * [onCompleted]: [confirmDeleteEntirePot]과 같은 이유로 안내를 콜백으로 넘긴다
     */
    fun completeStudyPlan(onCompleted: (notice: String?) -> Unit){
        if (potId.isEmpty()) return

        runWrite(
            write = { completeStudyPlanUseCase(potId) },
            onSaved = { result ->
                setCompleteDialogShown(false)
                onCompleted(result.noticeOrNull())
            }
        )
    }

    /**
     * 기록 삭제·화분 삭제·완료 공통 처리 (#455).
     * - 진행 중이면 다시 보내지 않고, 다이얼로그를 닫지 않으며 다이얼로그에 로딩을 표시한다 ([isWriting])
     *   (오프라인이면 서버 응답을 최대 5초 기다리므로 그동안 닫히면 다른 다이얼로그가 대신 닫히거나 화면이 갑자기 닫힘)
     * - 성공(오프라인 Pending 포함)이면 [onSaved]
     * - 실패면 안내 토스트만 띄우고 다이얼로그는 그대로 둔다 (다시 시도할 수 있게)
     *   세션 만료는 UseCase에서 처리(세션 만료 안내)하므로 토스트를 띄우지 않는다
     */
    private fun runWrite(
        write: suspend () -> Result<SaveResult>,
        onSaved: suspend (SaveResult) -> Unit
    ) {
        if (_isWriting.value) return
        _isWriting.value = true
        viewModelScope.launch {
            try {
                val result = write()
                // onSaved의 다이얼로그 닫기가 진행 중이라는 이유로 무시되지 않도록 먼저 해제
                _isWriting.value = false
                result
                    .onSuccess { saved -> onSaved(saved) }
                    .onFailure { error ->
                        if (error !is AppError.UnknownUser) {
                            _event.send(StudyPlanDetailEvent.ShowToast(error.message))
                        }
                    }
            } finally {
                // 화면을 나가 작업이 취소된 경우에도 해제
                _isWriting.value = false
            }
        }
    }

    private fun SaveResult.noticeOrNull(): String? =
        if (this == SaveResult.Pending) NETWORK_SLOW_MESSAGE else null

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