package com.a32b.plant.presentation.studying.viewmodel

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.getOrNull
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.type.StudyingSessionStatus
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.usecase.studying.FinishStudyingUseCase
import com.a32b.plant.domain.usecase.studying.GetRecoverySessionUseCase
import com.a32b.plant.domain.usecase.studying.GetStudyingGoalDraftUseCase
import com.a32b.plant.domain.usecase.studying.SaveStudyingGoalDraftUseCase
import com.a32b.plant.domain.usecase.studying.StartStudyingSessionUseCase
import com.a32b.plant.domain.usecase.studying.UpdateLocalStudyingSessionUseCase
import com.a32b.plant.presentation.core.type.StudyingGoalCheckMode
import com.a32b.plant.presentation.studying.StudyingConstants.MAX_STUDY_LOG_SIZE
import com.a32b.plant.presentation.studying.StudyingConstants.STUDY_LOG_LIMIT_MESSAGE
import com.a32b.plant.presentation.studying.StudyingConstants.STUDY_LOG_LIMIT_TOAST_INTERVAL_MS
import com.a32b.plant.presentation.studying.service.StudyingTimerService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime
import javax.inject.Inject

data class StudyLogUi(
    val log: String = "",
    val isCompleted: Boolean = false
)
data class StudyingUiState(
    val tag: String,
    val title: String,
    val level: String,
    val timer: Long = 0L,
    val isStudying: Boolean = false, //스톱워치 가동을 위한 공부중 여부 체크
    val buttonText: String = "일시정지",
    val studyingUsers: List<StudyingUser> = emptyList(),
    val studyLog: List<StudyLogUi> = emptyList(),
    val isLocalSaved: Boolean = true, // 로컬 저장 성공 여부 체크
    val startedAt: Long = 0L,
    val isLoading: Boolean = false,
    val isFinished: Boolean = false, // 종료 확정 여부 — 종료 저장을 시작하면 화면을 떠날 때까지 true 유지
    val error: String? = null,
    val isGoalInputDialogShown: Boolean = true, //학습 목표 입력
    val goalCheckMode: StudyingGoalCheckMode? = null,
    val wasStudyingBeforeGoalInputDialog: Boolean = true,
    val wasStudyingBeforeGoalCheckDialog: Boolean = false,
    val isEditing: Boolean = false,
    val logIndex: Int? = null,
    // 목표 입력 창에서 작성 중인 글 — 창 안 remember에 두면 화면 재생성·닫기로 사라지므로 ViewModel이 들고 있음
    val goalDraft: List<String> = listOf(""),
    val isGoalDiscardConfirmShown: Boolean = false, // 작성 중인 글이 있는 상태에서 닫기를 눌렀을 때 버릴지 확인
)

sealed class StudyingEvent{
    data class NavigateToStudyResult(
        val timestamp: String, //날짜 시작시간 ~ 종료 시간
        val tag: String,
        val title: String,
        val log: List<String>,
        val completed: List<Boolean>,
        val time: Long,
        val potId: String,
        val level: String,
        val sessionId: String,
        // 오프라인 등으로 서버 반영을 확인하지 못함 — 결과 화면에서 "연결되면 보상 지급" 안내
        val isSavePending: Boolean
        ): StudyingEvent()

    object NavigateToHome : StudyingEvent()
    data class ShowToast(val message: String, val duration: Int) : StudyingEvent()
}
@HiltViewModel
class StudyingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StudyingRepository,
    private val startStudyingSessionUseCase: StartStudyingSessionUseCase,
    private val updateLocalStudyingSessionUseCase: UpdateLocalStudyingSessionUseCase,
    private val finishStudyingUseCase: FinishStudyingUseCase,
    private val getRecoverySessionUseCase: GetRecoverySessionUseCase,
    private val userRepository: UserRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val saveStudyingGoalDraftUseCase: SaveStudyingGoalDraftUseCase,
    private val getStudyingGoalDraftUseCase: GetStudyingGoalDraftUseCase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {
    private val _eventChannel = Channel<StudyingEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    private val potId: String = savedStateHandle["potId"] ?: ""

    private val _uiState = MutableStateFlow(StudyingUiState(
        tag = savedStateHandle["tagName"] ?: "",
        title = savedStateHandle["title"] ?: "",
        level = savedStateHandle["level"] ?: ""
        )
    )
    val uiState = _uiState.asStateFlow()


    private var lastSaveMs = 0L
    private var lastUpdateMs = 0L
    private var savedGoalCheckMode: StudyingGoalCheckMode? = null
    // 최대 개수 안내 토스트를 마지막으로 띄운 시각 (elapsedRealtime 기준, 아직 안 띄웠으면 null)
    private var lastStudyLogLimitToastMs: Long? = null
    // 목표 임시 글 저장 작업 — 입력마다 쓰지 않도록 마지막 입력 후 일정 시간 뒤에 저장
    private var goalDraftSaveJob: Job? = null
    // 이 학습 세션의 ID — 학습 기록 문서 ID로 쓰여 같은 세션이 여러 번 저장 요청돼도 한 번만 반영된다 (#453)
    // 이어서 공부하는 세션이면 백업의 값으로 바꾸고, 백업마다 함께 저장한다
    private var sessionId: String = StudyingSession.newSessionId()

    init {
        viewModelScope.launch {
            // 종료 세션 차단과 이어서 공부 복원이 같은 백업을 보도록 한 번만 읽음 — 읽기 실패는 백업 없음과 같이 새 세션으로 시작
            val backup = repository.readLocalSession().getOrNull()
            // 종료했지만 저장되지 않은 세션을 새 공부가 덮어쓰지 않도록, 시작하지 않고 홈 복구 창으로 보냄
            if (isUnsavedFinishedSession(backup)) {
                sendToast("저장되지 않은 학습 기록이 있어 홈으로 이동합니다.")
                _eventChannel.send(StudyingEvent.NavigateToHome)
                return@launch
            }
            restoreFromLocalBackup(backup)
            // 로컬 복구 결과(타이머·목표 다이얼로그 표시 여부)가 확정된 뒤 화면 노출
            // 원격 등록(initStudyingUser)은 화면 표시와 무관하므로 기다리지 않음
            loaded()
            initStudyingUser()
        }
        onStudyingUsersChange()
        observeTimer()
    }

    /**
     * 홈 복구 창이 처리할 종료 세션인지 — 복구 대상 기준은 홈 복구 창과 같은 StudyingSession.isRecoverableBy 사용
     * 유저 정보가 없으면 막지 않고 진행 (기존과 동일). 세션 만료 알림은 이어지는 initStudyingUser가 보내므로
     * 알림이 두 번 가지 않도록 여기서는 ensureCurrentUserUseCase 대신 현재 값을 직접 읽음
     *
     * 종료 세션이면 홈 복구 창과 같은 판단(GetRecoverySessionUseCase)으로 한 번 더 확인한다 (#453)
     * - 저장 요청이 대기열에 남은 세션(오프라인 종료)은 연결되면 자동 저장되므로 막지 않는다
     *   → 오프라인에서도 이어서 여러 세션을 공부할 수 있다 (새 세션 백업이 덮어써도 대기열의 저장은 그대로 전송됨)
     * - 이미 서버에 저장된 세션은 판단 중에 백업이 지워지므로 막지 않는다
     */
    private suspend fun isUnsavedFinishedSession(session: StudyingSession?): Boolean {
        if (session == null || session.status != StudyingSessionStatus.FINISHED) return false
        val uid = userRepository.currentUser.value?.uid ?: return false
        if (!session.isRecoverableBy(uid)) return false
        return getRecoverySessionUseCase().getOrNull()?.status == StudyingSessionStatus.FINISHED
    }

    private suspend fun restoreFromLocalBackup(session: StudyingSession?) {
        // 종료한 세션은 이어서 공부하지 않으므로 공부 중이던 세션만 복원
        if (session == null || session.potId != potId || session.status != StudyingSessionStatus.STUDYING) return
        // 이어서 공부하는 세션은 같은 세션 ID를 쓴다 (이전 버전 백업은 값이 없어 새 ID 유지)
        if (session.sessionId.isNotBlank()) sessionId = session.sessionId
        // 목표 입력 단계(0초, 목표 없음)에서 앱이 종료된 세션 — 처음 시작할 때처럼 타이머를 멈춘 채
        // 목표 입력 창을 다시 띄우고 작성 중이던 글을 복원 (시간이 쌓인 세션은 아래에서 기존대로 복원, #253)
        if (session.time == 0L && session.log.orEmpty().none { it.isNotBlank() }) {
            restoreGoalDraft()
            return
        }
        lastSaveMs = session.time
        lastUpdateMs = session.time
        _uiState.update {
            it.copy(
                timer = session.time,
                startedAt = session.startedAt,
                // 완료 체크도 함께 복원 (이전 버전 백업은 완료 상태가 없어 미완료)
                studyLog = session.log
                    ?.mapIndexed { index, log -> StudyLogUi(log = log, isCompleted = session.completed.getOrNull(index) ?: false) }
                    ?.filter { it.log.isNotBlank() }
                    ?: emptyList(),
                isGoalInputDialogShown = false,
                isStudying = !session.isPaused
            )
        }
        // 일시정지 중이었으면 사용자가 재개할 때까지 타이머를 시작하지 않는다
        if (!session.isPaused) startStopwatch()
    }

    /** 같은 계정·같은 화분에서 작성 중이던 목표 임시 글을 입력 창에 복원 — 읽기 실패나 글이 없으면 빈 입력 창 그대로 */
    private suspend fun restoreGoalDraft() {
        getStudyingGoalDraftUseCase(potId).getOrNull()?.let { logs ->
            _uiState.update { it.copy(goalDraft = logs.take(MAX_STUDY_LOG_SIZE)) }
        }
    }

    /** 목표 입력 창 글 변경 — 처음 입력 창의 글만 로컬에 임시 저장 (수정 창은 재진입 시 다시 열리지 않으므로 저장하지 않음) */
    fun onGoalDraftChange(logs: List<String>) {
        _uiState.update { it.copy(goalDraft = logs) }
        if (_uiState.value.isEditing) return
        goalDraftSaveJob?.cancel()
        goalDraftSaveJob = viewModelScope.launch {
            delay(GOAL_DRAFT_SAVE_DELAY_MS)
            saveStudyingGoalDraftUseCase(potId, logs) // 실패해도 입력·공부에는 영향 없으므로 별도 처리 없음
        }
    }

    /** 목표 임시 글 삭제 — 대기 중인 저장이 삭제 뒤에 실행돼 글이 되살아나지 않도록 먼저 취소 */
    private fun clearGoalDraft() {
        val pendingSave = goalDraftSaveJob
        goalDraftSaveJob = null
        viewModelScope.launch {
            pendingSave?.cancelAndJoin()
            repository.clearLocalGoalDraft() // 실패해도 다음 입력에서 덮어쓰고, 다른 화분·계정 글은 조회 시 걸러지므로 별도 처리 없음
        }
    }

    /** 목표 입력 창 닫기 버튼 — 작성한 내용이 있으면 버릴지 먼저 확인 */
    fun onGoalInputDialogCloseRequest() {
        val state = _uiState.value
        val hasContent = if (state.isEditing) {
            // 수정 창은 기존 목표가 채워져 있으므로 원래 글에서 바뀌었을 때만 확인
            val index = state.logIndex
            index != null && state.goalDraft.getOrNull(index).orEmpty() != state.studyLog.getOrNull(index)?.log.orEmpty()
        } else {
            state.goalDraft.any { it.isNotBlank() }
        }
        if (hasContent) _uiState.update { it.copy(isGoalDiscardConfirmShown = true) }
        else onGoalInputDialogChange(false)
    }

    /** 버리기 확인 창에서 취소 — 입력 창과 작성 중인 글 유지 */
    fun onGoalDiscardCancel() = _uiState.update { it.copy(isGoalDiscardConfirmShown = false) }

    /** 버리기 확인 창에서 버리기 — 입력 창을 닫고 임시 글 삭제 */
    fun onGoalDiscardConfirm() {
        _uiState.update { it.copy(isGoalDiscardConfirmShown = false) }
        onGoalInputDialogChange(false)
    }

    private fun observeTimer() {
        viewModelScope.launch {
            StudyingTimerService.elapsed.collect { elapsed ->
                if (_uiState.value.isStudying) {
                    _uiState.update { it.copy(timer = elapsed) }
                    if (elapsed - lastSaveMs >= 5_000L) {
                        lastSaveMs = elapsed
                        saveSession()
                    }
                    if (elapsed - lastUpdateMs >= 60_000L) {
                        lastUpdateMs = elapsed
                        updateUser()
                    }
                }
            }
        }
    }

    /**
     * 학습 시작 시각 기록 — 아직 값이 없을 때만 넣는다
     * 화면이 다시 그려질 때마다 호출되므로, 이어서 공부하는 세션이 백업에서 복원한 시작 시각을
     * 화면을 연 시각으로 덮어쓰지 않게 한다 (덮어쓰면 startedAt + 학습 시간이 미래가 되어 보안 규칙이 저장을 거부, #453)
     */
    fun onStartedAtChange(value : Long) = _uiState.update { if (it.startedAt == 0L) it.copy(startedAt = value) else it }

    /** 비정상 종료 대비 로컬 디비에 데이터 저장   */
    private suspend fun saveSession(status: StudyingSessionStatus = StudyingSessionStatus.STUDYING){
        val state = _uiState.value
        updateLocalStudyingSessionUseCase(
            tag = state.tag,
            title = state.title,
            potId = potId,
            time = state.timer,
            log = state.studyLog.map { it.log },
            completed = state.studyLog.map { it.isCompleted },
            startedAt = state.startedAt,
            isPaused = !state.isStudying,
            sessionId = sessionId,
            status = status
        )
            .onSuccess { _uiState.update { it.copy(isLocalSaved = true) } }
            .onFailure { e ->
                if (e is AppError.Custom) _uiState.update { it.copy(isLocalSaved = false) }
            }
    }

    /**
     * 최초 시작 시 로컬 + 원격 db에 현재 사용자 정보 저장
     * 실패해도 공부는 계속하므로 별도 처리 없음 (UnknownUser는 유즈케이스에서 처리, 에러 로그는 레포지토리에서 찍고 있음)
     */
    suspend fun initStudyingUser(){
        withContext(Dispatchers.IO){
            val state = _uiState.value
            startStudyingSessionUseCase(
                tag = state.tag,
                title = state.title,
                potId = potId,
                time = state.timer,
                log = state.studyLog.map { it.log },
                completed = state.studyLog.map { it.isCompleted },
                startedAt = state.startedAt,
                isPaused = !state.isStudying,
                sessionId = sessionId
            )
        }
    }

    /** db에서 같은 태그로 공부중인 사용자 데이터 가져오기 */
    fun onStudyingUsersChange(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeStudyingUser(_uiState.value.tag)
                .collect { users ->
                    _uiState.update { it.copy(studyingUsers = users) }
                }
        }
    }

    /**
     * [DEBUG] 타이머에 1시간(3,600,000ms) 추가 — 보상 테스트용, 릴리즈 전 제거
     * 시작 시각도 1시간 앞당긴다. 타이머만 늘리면 startedAt + 학습 시간이 실제 시각보다 1시간 늦어져
     * 보안 규칙이 "미래 종료"로 저장을 거부한다 (#453)
     */
    fun addDebugTime() {
        val newTimer = _uiState.value.timer + 3_600_000L
        lastSaveMs = newTimer
        lastUpdateMs = newTimer
        if (_uiState.value.isStudying) {
            _uiState.update { it.copy(startedAt = it.startedAt - 3_600_000L) }
            sendServiceIntent(StudyingTimerService.ACTION_START) {
                putExtra(StudyingTimerService.EXTRA_ACCUMULATED, newTimer)
            }
        }
    }

    private fun startStopwatch() {
        // 재개 직후 앱이 종료돼도 일시정지 상태로 복원되지 않도록 바로 저장한다
        viewModelScope.launch { saveSession() }
        sendServiceIntent(StudyingTimerService.ACTION_START) {
            putExtra(StudyingTimerService.EXTRA_ACCUMULATED, _uiState.value.timer)
        }
    }

    private fun stopStopwatch() {
        viewModelScope.launch { saveSession() }
        sendServiceIntent(StudyingTimerService.ACTION_PAUSE)
    }

    private fun sendServiceIntent(action: String, extras: (Intent.() -> Unit)? = null) {
        val intent = Intent(context, StudyingTimerService::class.java).apply {
            this.action = action
            extras?.invoke(this)
        }
        if (action == StudyingTimerService.ACTION_START) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }

    /** 공부중 상태 변경 t: studying f:!studying */
    fun onStudyingStatusChange(studying: Boolean = !_uiState.value.isStudying) {
         _uiState.update { it.copy(isStudying = studying) }

        if(studying) startStopwatch()
        else stopStopwatch()
    }

    /** 유저 정보 업데이트 */
    private fun updateUser(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStudyingTime(_uiState.value.tag, _uiState.value.timer)
                .onFailure { if (it is AppError.UnknownUser) ensureCurrentUserUseCase() } //유저 정보 없을 시 로그아웃 처리
        }

    }

    /**  학습 목표 설정 */
    fun setStudyLog(studyLog: List<String>) {
        if (studyLog.isEmpty()) return
        val current = _uiState.value.studyLog

        _uiState.update {
            it.copy(studyLog = studyLog
                .mapIndexed { index, string ->
                    val existing = current.getOrNull(index)
                    StudyLogUi(log = string, isCompleted = existing?.isCompleted ?: false)
                }
                .filter { studyLog -> studyLog.log.isNotBlank() }
            )
        }
        viewModelScope.launch { saveSession() }
    }

    fun onStartEditing(index: Int) {
        // 연타 등으로 이미 편집 다이얼로그가 열려 있으면 무시 — 두 번째 호출이 savedGoalCheckMode를 null로 덮어쓰는 것도 방지
        if (_uiState.value.isEditing) return
        // savedGoalCheckMode 저장 / 확인 다이얼로그 닫기보다 먼저 막아야 확인 다이얼로그가 그대로 유지됨
        if (index == -1 && blockIfStudyLogFull()) return
        savedGoalCheckMode = _uiState.value.goalCheckMode
        _uiState.update { state ->
            val newState = if (index == -1) {
                val newLog = state.studyLog + StudyLogUi()
                state.copy(studyLog = newLog, isEditing = true, logIndex = newLog.lastIndex)
            } else {
                state.copy(isEditing = true, logIndex = index)
            }
            newState.copy(
                isGoalInputDialogShown = true,
                goalCheckMode = null,
                goalDraft = newState.studyLog.map { it.log }
            )
        }
    }

    /** 학습 목표 설정 다이얼로그 표출 여부 제어*/
    fun onGoalInputDialogChange(value: Boolean) {
        if (!_uiState.value.isEditing) {
            if (value) {
                val wasStudying = _uiState.value.isStudying
                _uiState.update { it.copy(
                    isGoalInputDialogShown = true,
                    wasStudyingBeforeGoalInputDialog = wasStudying,
                    goalDraft = it.studyLog.map { log -> log.log }.ifEmpty { listOf("") }
                ) }
                onStudyingStatusChange(false)
            } else {
                val restore = _uiState.value.wasStudyingBeforeGoalInputDialog
                _uiState.update { it.copy(isGoalInputDialogShown = false, goalDraft = listOf("")) }
                // 완료·닫기 모두 입력이 끝났으므로 임시 글 삭제
                clearGoalDraft()
                onStudyingStatusChange(restore)
            }
        } else {
            if (value) {
                savedGoalCheckMode = _uiState.value.goalCheckMode
                _uiState.update { it.copy(
                    isGoalInputDialogShown = true,
                    goalCheckMode = null
                )}
            } else {
                _uiState.update { it.copy(
                    isGoalInputDialogShown = false,
                    goalCheckMode = savedGoalCheckMode,
                    goalDraft = listOf("")
                )}
                savedGoalCheckMode = null
                finishEditing()
                // 일시정지 중에는 주기 저장이 돌지 않으므로 수정한 목표를 바로 백업한다
                viewModelScope.launch { saveSession() }
            }
        }
    }

    /** 학습 목표 확인 다이얼로그 표출 모드 제어*/
    fun onChangeGoalCheckMode(mode: StudyingGoalCheckMode?) {
        // 종료 저장 중·저장 후 뒤로 가기 등으로 확인 창이 다시 열려 종료가 한 번 더 실행되지 않도록 무시
        // 닫기(null)도 막음 — 통과하면 종료 후 타이머가 재개되고 FINISHED 백업이 STUDYING으로 덮임
        if (_uiState.value.isFinished) return
        if (mode != null) {
            if (mode == StudyingGoalCheckMode.FINISH) {
                val wasStudying = _uiState.value.isStudying
                _uiState.update { it.copy(goalCheckMode = mode, wasStudyingBeforeGoalCheckDialog = wasStudying) }
                if (wasStudying) onStudyingStatusChange(false)
            } else {
                _uiState.update { it.copy(goalCheckMode = mode) }
            }
        } else {
            val previousMode = _uiState.value.goalCheckMode
            val restore = _uiState.value.wasStudyingBeforeGoalCheckDialog
            _uiState.update { it.copy(goalCheckMode = null) }
            if (previousMode == StudyingGoalCheckMode.FINISH && restore) {
                onStudyingStatusChange(true)
            }
        }
    }

    /** 편집 종료 — 방금 새로 추가한 목표를 저장하지 않고 닫았으면(빈 목표) 마지막 목표 제거 */
    private fun finishEditing() {
        _uiState.update { state ->
            val isUnsavedNewLog = state.logIndex == state.studyLog.lastIndex && state.studyLog.lastOrNull()?.log?.isBlank() == true
            state.copy(
                studyLog = if (isUnsavedNewLog) state.studyLog.dropLast(1) else state.studyLog,
                isEditing = false,
                logIndex = null
            )
        }
    }

    /**
     * 최대 개수 이상이면 새 목표 추가를 막고 안내 토스트 발송 — 막았으면 true 반환
     * (복구 등으로 이미 초과한 목록은 지우지 않고 추가만 막음)
     * 연타 시 토스트가 쌓이지 않도록 이전 안내가 떠 있는 동안(LENGTH_LONG)은 토스트만 생략하고 추가는 계속 막음
     */
    private fun blockIfStudyLogFull(): Boolean {
        if (_uiState.value.studyLog.size < MAX_STUDY_LOG_SIZE) return false
        val now = SystemClock.elapsedRealtime()
        val last = lastStudyLogLimitToastMs
        if (last == null || now - last >= STUDY_LOG_LIMIT_TOAST_INTERVAL_MS) {
            lastStudyLogLimitToastMs = now
            sendToast(STUDY_LOG_LIMIT_MESSAGE, Toast.LENGTH_LONG)
        }
        return true
    }

    /** 목표 수정 함수 */
    fun onStudyLogChanged(index: Int, completed: Boolean? = null, log: String? = null) = _uiState.update {
        it.copy(
            // 공백만 입력한 목표도 빈 입력과 같이 삭제 — 완료 체크는 log = null로 호출되므로 삭제 대상 아님
            studyLog = if (log != null && log.isBlank()) {
                it.studyLog.filterIndexed { i, _ -> i != index }
            } else {
                it.studyLog.mapIndexed { i, item ->
                    if (i == index) {
                        item.copy(
                            isCompleted = completed ?: item.isCompleted,
                            log = log ?: item.log
                        )
                    } else {
                        item
                    }
                }
            }
        )
    }

    /** 학습 완전 종료 시 (= 다이얼로그에서도 기록 입력 후 종료 버튼 클릭했을 때)    */
    fun onIsStudyFinishChange() {
        // 종료 버튼 연타 시 학습 기록·학습 시간이 두 번 저장되지 않도록 한 번만 실행
        // isLoading은 저장이 끝나면 풀리므로(결과 화면 이동 전) 별도 플래그로 막음
        if (_uiState.value.isFinished) return
        sendServiceIntent(StudyingTimerService.ACTION_STOP)
        _uiState.update { it.copy(isLoading = true, isFinished = true, goalCheckMode = null) }
        onFinishStudyingClick()
    }

    private fun getCurrentTime(): String{
        val now = LocalDateTime.now()
        return TimeFormatter.formatToTimeOnly(now)
    }

    /** 학습 내역 저장 및 결과창으로 이동 */
    private fun onFinishStudyingClick() {
        val startTime = TimeFormatter.formatToTimeOnly(_uiState.value.startedAt)
        //개별 학습 기록의 제목
        val timestamp = "${TimeFormatter.formatToKoreanDate(LocalDateTime.now())} ${startTime} ~ ${getCurrentTime()}"
        val resultTimestamp = "${TimeFormatter.formatWithDayOfWeek(LocalDateTime.now())} ${startTime} ~ ${getCurrentTime()}"

        viewModelScope.launch(Dispatchers.IO){
            // 저장 요청 전에 종료 상태로 백업 — 저장이 끝나지 않은 채 화면을 떠나도 이어서 공부하지 않고 홈 복구 창에서 처리
            // (저장 요청이 대기열에 남아 있으면 홈 복구 창이 서버 반영을 확인한 뒤 처리한다)
            saveSession(StudyingSessionStatus.FINISHED)
            // "공부 중" 표시는 저장 결과와 상관없이 삭제 요청 (오프라인이면 연결 후 반영)
            deleteStudyingUserInfo()

            val state = _uiState.value
            var isSavePending = false
            var canShowResult = true
            finishStudyingUseCase(
                potId = potId,
                sessionId = sessionId,
                timestamp = timestamp,
                log = state.studyLog.map { it.log },
                completed = state.studyLog.map { it.isCompleted },
                time = state.timer,
                startedAt = state.startedAt
            )
                .onSuccess { result ->
                    when (result) {
                        StudySaveResult.Saved, StudySaveResult.AlreadySaved -> clearLocalBackup()
                        // 실패가 아니다: 기기에는 이미 반영됐고 연결되면 자동 전송된다. 백업은 서버 반영 확인 전까지 유지
                        StudySaveResult.Pending -> {
                            isSavePending = true
                            sendToast(SAVE_PENDING_MESSAGE, Toast.LENGTH_LONG)
                        }
                        // 다시 보내도 저장되지 않으므로 백업을 지워 복구 창이 반복되지 않게 함
                        is StudySaveResult.Rejected -> {
                            canShowResult = false
                            clearLocalBackup()
                            _uiState.update { it.copy(error = result.reason.message) }
                        }
                    }
                }
                .onFailure { e ->
                    // 백업은 남겨 홈 복구 창에서 다시 저장할 수 있게 함 (같은 세션 ID라 중복 저장되지 않음)
                    canShowResult = false
                    when (e){
                        is AppError.UnknownUser -> ensureCurrentUserUseCase()
                        else -> _uiState.update { it.copy(error = e.message) }
                    }
                }

            _uiState.update { it.copy(isLoading = false) }

            if (canShowResult){
                _eventChannel.send(StudyingEvent.NavigateToStudyResult(
                    timestamp = resultTimestamp,
                    tag = state.tag,
                    potId = potId,
                    title = state.title,
                    log = state.studyLog.map { it.log },
                    completed = state.studyLog.map { it.isCompleted },
                    time = state.timer,
                    level = state.level,
                    sessionId = sessionId,
                    isSavePending = isSavePending
                ))
            }

        }
    }

    private suspend fun clearLocalBackup(){
        repository.clearLocalSession()
            .onFailure { sendToast("자동 저장된 학습 기록이 지워지지 않았습니다.") }
    }

    /**
     * "공부 중" 표시(studying/{uid}) 삭제 — 오프라인이면 연결 후 반영되므로 완료를 기다리지 않음 (HomeViewModel과 같은 방식)
     * 삭제 요청은 호출 즉시 대기열에 들어가므로 화면을 떠나 코루틴이 취소돼도 전송된다
     * 실패해도 저장 결과에는 영향이 없어 별도 처리 없음 (에러 로그는 레포지토리에서 찍고 있음)
     */
    private fun deleteStudyingUserInfo() {
        viewModelScope.launch { repository.deleteStudyingUserInfo() }
    }

    /**
     * 에러 다이얼로그 확인 — 기다리지 않고 바로 홈으로 이동
     * "공부 중" 표시 삭제는 종료 시 이미 요청했지만, 연결이 끊긴 사이 실패했을 수 있어 다시 요청만 함
     * 로컬 백업은 저장 실패면 복구용으로 남아 있고, 다시 보내도 안 되는 거부면 이미 지워졌다
     */
    fun onErrorConfirmClicked(){
        sendServiceIntent(StudyingTimerService.ACTION_STOP)
        deleteStudyingUserInfo()
        _uiState.update { it.copy(error = null) }
        viewModelScope.launch { _eventChannel.send(StudyingEvent.NavigateToHome) }
    }

    private fun sendToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        viewModelScope.launch { _eventChannel.send(StudyingEvent.ShowToast(message, duration)) }
    }

    companion object {
        // 마지막 입력 후 임시 저장까지 기다리는 시간
        private const val GOAL_DRAFT_SAVE_DELAY_MS = 500L
        private const val SAVE_PENDING_MESSAGE = "네트워크에 연결되면 학습 기록이 자동으로 저장돼요."
    }

}
