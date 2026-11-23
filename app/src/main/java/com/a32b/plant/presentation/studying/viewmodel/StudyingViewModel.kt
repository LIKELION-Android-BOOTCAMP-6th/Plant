package com.a32b.plant.presentation.studying.viewmodel

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.a32b.plant.BuildConfig
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.getOrNull
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.type.StudySaveRejection
import com.a32b.plant.domain.type.StudyingSessionStatus
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.usecase.studying.ClaimDailyNotificationGuideUseCase
import com.a32b.plant.domain.usecase.studying.FinishStudyingUseCase
import com.a32b.plant.domain.usecase.studying.GetRecoverySessionUseCase
import com.a32b.plant.domain.usecase.studying.GetStudyingGoalDraftUseCase
import com.a32b.plant.domain.usecase.studying.ObserveStudyingUsersUseCase
import com.a32b.plant.domain.usecase.studying.SaveStudyingGoalDraftUseCase
import com.a32b.plant.domain.usecase.studying.SaveStudyingUserUseCase
import com.a32b.plant.domain.usecase.studying.StartStudyingSessionUseCase
import com.a32b.plant.domain.usecase.studying.UpdateLocalStudyingSessionUseCase
import com.a32b.plant.presentation.core.type.StudyingGoalCheckMode
import com.a32b.plant.presentation.studying.NotificationAccess
import com.a32b.plant.presentation.studying.StudyingConstants.LOCAL_BACKUP_INTERVAL_MS
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
    val isNotificationGuideShown: Boolean = false, // 알림이 꺼져 있어 설정 화면으로 안내 (#610)
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
    object OpenNotificationSettings : StudyingEvent()
}
@HiltViewModel
class StudyingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: StudyingRepository,
    private val startStudyingSessionUseCase: StartStudyingSessionUseCase,
    private val saveStudyingUserUseCase: SaveStudyingUserUseCase,
    private val updateLocalStudyingSessionUseCase: UpdateLocalStudyingSessionUseCase,
    private val finishStudyingUseCase: FinishStudyingUseCase,
    private val getRecoverySessionUseCase: GetRecoverySessionUseCase,
    private val userRepository: UserRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val saveStudyingGoalDraftUseCase: SaveStudyingGoalDraftUseCase,
    private val getStudyingGoalDraftUseCase: GetStudyingGoalDraftUseCase,
    private val observeStudyingUsersUseCase: ObserveStudyingUsersUseCase,
    private val claimDailyNotificationGuideUseCase: ClaimDailyNotificationGuideUseCase,
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


    // 7일 경과 확인(stopIfSessionTooOld)을 마지막으로 한 학습 시간 — 주기 저장은 타이머 서비스가 한다 (#668)
    private var lastTooOldCheckMs = 0L
    private var lastUpdateMs = 0L
    private var savedGoalCheckMode: StudyingGoalCheckMode? = null
    // 최대 개수 안내 토스트를 마지막으로 띄운 시각 (elapsedRealtime 기준, 아직 안 띄웠으면 null)
    private var lastStudyLogLimitToastMs: Long? = null
    // 목표 임시 글 저장 작업 — 입력마다 쓰지 않도록 마지막 입력 후 일정 시간 뒤에 저장
    private var goalDraftSaveJob: Job? = null
    // 이 학습 세션의 ID — 학습 기록 문서 ID로 쓰여 같은 세션이 여러 번 저장 요청돼도 한 번만 반영된다 (#453)
    // 이어서 공부하는 세션이면 백업의 값으로 바꾸고, 백업마다 함께 저장한다
    private var sessionId: String = StudyingSession.newSessionId()
    // 일시정지 후 "공부 중" 표시 삭제 예약 (#667)
    // 메인 스레드에서만 읽고 쓴다 — 예약은 viewModelScope, 취소는 메인에서 불리는 startStopwatch와
    // deleteStudyingUserInfo의 viewModelScope 안에서 한다 (IO에서 부르는 종료 저장 경로 포함)
    // 대기 중인지 확인은 메인에서 불리는 onScreenStopped(화면 ON_STOP)에서 한다
    private var studyingUserDeleteJob: Job? = null

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
            loaded()
            // 복원한 세션이 저장할 수 없는 세션이라 멈췄으면(stopIfSessionTooOld) 지운 백업을 다시 쓰지 않는다
            saveInitialSession()
        }
        observeStudyingUsers()
        observeTimer()
        observeServiceSaveResult()
    }

    /**
     * 홈 복구 창이 처리할 종료 세션인지 — 복구 대상 기준은 홈 복구 창과 같은 StudyingSession.isRecoverableBy 사용
     * 유저 정보가 없으면 막지 않고 진행 (기존과 동일). 세션 만료 알림은 이어지는 saveInitialSession이 보내므로
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
        lastTooOldCheckMs = session.time
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
        // 일시정지 중이던 세션도 재개를 누르기 전에 저장할 수 있는 세션인지 먼저 확인한다 (#633)
        if (stopIfSessionTooOld()) return
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
                    // 주기 저장은 타이머 서비스가 하고(#668), 여기서는 같은 주기로 7일 경과만 확인한다
                    if (elapsed - lastTooOldCheckMs >= LOCAL_BACKUP_INTERVAL_MS) {
                        lastTooOldCheckMs = elapsed
                        // 공부하는 동안 시작한 지 7일을 넘기면 더 쌓지 않고 멈춘다 (#633)
                        if (stopIfSessionTooOld()) return@collect
                    }
                    if (elapsed - lastUpdateMs >= 60_000L) {
                        lastUpdateMs = elapsed
                        updateUser()
                    }
                }
            }
        }
    }

    /** 타이머 서비스의 주기 저장 결과를 저장 실패 안내에 반영 — 화면 저장(saveSession)과 같은 기준 */
    private fun observeServiceSaveResult() {
        viewModelScope.launch {
            StudyingTimerService.localSaveResult.collect { saved ->
                _uiState.update { it.copy(isLocalSaved = saved) }
            }
        }
    }

    /**
     * 학습 시작 시각 기록 — 아직 값이 없을 때만 넣는다
     * 화면이 다시 그려질 때마다 호출되므로, 이어서 공부하는 세션이 백업에서 복원한 시작 시각을
     * 화면을 연 시각으로 덮어쓰지 않게 한다 (덮어쓰면 startedAt + 학습 시간이 미래가 되어 보안 규칙이 저장을 거부, #453)
     */
    fun onStartedAtChange(value : Long) = _uiState.update { if (it.startedAt == 0L) it.copy(startedAt = value) else it }

    /**
     * 비정상 종료 대비 로컬 디비에 데이터 저장
     * 종료가 확정된 뒤(isFinished)에는 공부 중(STUDYING) 백업을 쓰지 않는다 — 종료 저장의 FINISHED 백업을 덮거나,
     * 저장할 수 없는 세션이라 지운 백업(stopIfSessionTooOld)을 되살리지 않게 한다
     */
    private suspend fun saveSession(status: StudyingSessionStatus = StudyingSessionStatus.STUDYING){
        val state = _uiState.value
        if (status == StudyingSessionStatus.STUDYING && state.isFinished) return
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
     * 최초 진입 시 로컬 백업 저장 — "공부 중" 표시는 타이머가 시작될 때 등록한다 (startStopwatch, #525)
     * 실패해도 공부는 계속하므로 별도 처리 없음 (UnknownUser는 유즈케이스에서 처리)
     */
    private suspend fun saveInitialSession(){
        if (_uiState.value.isFinished) return
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

    /**
     * db에서 같은 태그로 공부중인 사용자 데이터 가져오기 — 유령(강제 종료로 남은 문서)은 유즈케이스가 뺀다 (#618)
     * 부를 때마다 새 수집을 시작하므로 init에서만 부른다 (#730)
     * 태그는 savedStateHandle에서 받아 init 시점에 정해지고, 이어서 공부하기 복원(restoreFromLocalBackup)도 태그를 바꾸지 않는다
     */
    private fun observeStudyingUsers(){
        viewModelScope.launch(Dispatchers.IO) {
            observeStudyingUsersUseCase(_uiState.value.tag)
                .collect { users ->
                    _uiState.update { it.copy(studyingUsers = users) }
                }
        }
    }

    /**
     * [DEBUG] 타이머에 1시간(3,600,000ms) 추가 — 보상 테스트용, 디버그 빌드에서만 동작한다
     * 사용자용(릴리즈) 빌드에서는 버튼을 그리지 않고, 다른 곳에서 불러도 아무것도 바꾸지 않고 반환한다 (#590)
     * 시작 시각도 1시간 앞당긴다. 타이머만 늘리면 startedAt + 학습 시간이 실제 시각보다 1시간 늦어져
     * 보안 규칙이 "미래 종료"로 저장을 거부한다 (#453)
     */
    fun addDebugTime() {
        if (!BuildConfig.DEBUG) return
        val newTimer = _uiState.value.timer + 3_600_000L
        lastTooOldCheckMs = newTimer
        lastUpdateMs = newTimer
        if (_uiState.value.isStudying) {
            _uiState.update { it.copy(startedAt = it.startedAt - 3_600_000L, timer = newTimer) }
            // 서비스의 주기 저장은 시간만 쓰므로 앞당긴 시작 시각을 바로 백업한다 (#668)
            viewModelScope.launch { saveSession() }
            sendServiceIntent(StudyingTimerService.ACTION_START) {
                putExtra(StudyingTimerService.EXTRA_ACCUMULATED, newTimer)
            }
        }
    }

    private fun startStopwatch() {
        // 멈춘 타이머를 시작·재개하는 경로(복원, 재개 버튼, 목표·종료 확인 창 닫기)는 모두 여기를 지나므로,
        // 저장할 수 없는 세션이면 시작하지 않는다 (#633)
        // addDebugTime은 공부 중일 때만 서비스를 다시 시작하므로 여기를 거치지 않고, 주기 확인(observeTimer)에서 걸린다
        if (stopIfSessionTooOld()) return
        // 재개 직후 앱이 종료돼도 일시정지 상태로 복원되지 않도록 바로 저장한다
        viewModelScope.launch { saveSession() }
        // 일시정지 때 걸어 둔 삭제 예약을 쓰기보다 먼저 취소한다 — 같은 메인 스레드에서 취소 → 쓰기 순서 (#667)
        cancelScheduledStudyingUserDelete()
        // "공부 중" 표시를 바로 등록하고(일시정지로 지웠거나 처음 시작) 1분 주기도 지금부터 센다 (#525)
        lastUpdateMs = _uiState.value.timer
        updateUser()
        sendServiceIntent(StudyingTimerService.ACTION_START) {
            putExtra(StudyingTimerService.EXTRA_ACCUMULATED, _uiState.value.timer)
        }
    }

    private fun stopStopwatch() {
        viewModelScope.launch { saveSession() }
        sendServiceIntent(StudyingTimerService.ACTION_PAUSE)
    }

    /**
     * 시작한 지 오프라인 동기화 허용 기간(StudyLog.MAX_OFFLINE_SYNC_DAYS)이 지나 더 공부해도 저장할 수 없는 세션이면
     * 멈추고 안내한다 — 멈췄으면 true (#633)
     * 일시정지 시간에는 상한이 없어서, 종료를 눌러서야 거부되면 이어서 공부한 시간까지 함께 잃는다.
     * 그래서 타이머 시작·재개(startStopwatch), 복원(restoreFromLocalBackup), 공부 중 주기 확인(observeTimer)에서 미리 확인한다
     * - isStudying·isFinished를 먼저 바꿔 주기 저장·"공부 중" 갱신(updateUser)·재개가 멈춘 뒤에 백업을 지운다
     *   (stopStopwatch는 saveSession을 불러 백업을 다시 쓰므로 쓰지 않고 서비스만 멈춤)
     *   (멈추기 직전 출발한 서비스 저장이 늦게 도착해도 백업이 없으면 새로 만들지 않는다 — updateLocalSessionTime)
     * - 시작 시각이 아직 없으면(0) 판단하지 않는다 — 종료 시 FinishStudyingUseCase가 다시 확인한다
     * - 기준은 FinishStudyingUseCase의 사전 검사와 같은 StudyLog.isTooOldToSave (기기 시계 기준)
     */
    private fun stopIfSessionTooOld(): Boolean {
        val state = _uiState.value
        if (state.isFinished || state.startedAt == 0L) return false
        if (!StudyLog.isTooOldToSave(state.startedAt, System.currentTimeMillis())) return false

        sendServiceIntent(StudyingTimerService.ACTION_STOP)
        _uiState.update {
            it.copy(
                isStudying = false,
                isFinished = true,
                goalCheckMode = null,
                isGoalInputDialogShown = false,
                isGoalDiscardConfirmShown = false
            )
        }
        deleteStudyingUserInfo()
        viewModelScope.launch {
            // 다시 보내도 저장되지 않으므로 종료 시 거부(Rejected)와 같이 백업을 지워 복구 창이 반복되지 않게 함
            clearLocalBackup()
            _uiState.update { it.copy(error = StudySaveRejection.TOO_OLD.message) }
        }
        return true
    }

    private fun sendServiceIntent(action: String, extras: (Intent.() -> Unit)? = null) {
        val intent = Intent(context, StudyingTimerService::class.java).apply {
            this.action = action
            // 서비스가 주기 저장할 때 이 세션의 백업인지 확인하는 기준 (#668)
            if (action == StudyingTimerService.ACTION_START) putExtra(StudyingTimerService.EXTRA_SESSION_ID, sessionId)
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
        // 종료가 확정된 뒤(종료 저장 중, 저장할 수 없는 세션이라 멈춤)에는 목표 창 닫기 등으로 타이머가 다시 켜지지 않게 무시
        if (_uiState.value.isFinished) return
        val wasStudying = _uiState.value.isStudying
         _uiState.update { it.copy(isStudying = studying) }

        if(studying) startStopwatch()
        else stopStopwatch()
        // 일시정지 중에는 "공부 중" 상위 3명에 표시하지 않는다 → 공부 중이다가 멈출 때만 삭제 (#525)
        // (이미 멈춘 상태에서 다이얼로그를 열고 닫을 때도 false로 호출되므로 상태가 바뀔 때만)
        // 바로 지우면 일시정지·학습하기를 연타할 때 상대 화면 목록이 깜빡이므로 일정 시간 뒤에 지운다 (#667)
        if (wasStudying && !studying) scheduleStudyingUserDelete()
    }

    /**
     * "공부 중" 표시 등록·갱신 — 공부 중일 때만 (일시정지·종료 후 늦게 실행돼 지운 문서를 되살리지 않게)
     * Dispatchers.IO로 옮기지 않는다: 메인 스레드에서 상태 확인과 쓰기 요청이 끊김 없이 이어져야
     * 일시정지 삭제(scheduleStudyingUserDelete의 예약 삭제·deleteStudyingUserInfo)와 순서가 뒤바뀌지 않는다 (쓰기 요청 자체는 메인을 막지 않음)
     * 이 보장은 set() 호출 전에 코루틴이 멈추는 지점이 없다는 전제 위에 있다 — SaveStudyingUserUseCase의
     * ensureCurrentUserUseCase()는 suspend가 아니고 Repository의 safeRunCatching은 inline이다.
     * 이 경로(UseCase·Repository·RemoteDataSource)에 withContext·delay 등을 넣으면 순서가 깨질 수 있다
     * 실패해도 다음 주기에 다시 쓰므로 별도 처리 없음 (유저 정보가 없으면 유즈케이스가 세션 만료 처리)
     */
    private fun updateUser(){
        viewModelScope.launch {
            val state = _uiState.value
            if (!state.isStudying || state.isFinished) return@launch
            saveStudyingUserUseCase(state.tag, state.timer)
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

    /**
     * 목표 수정 함수
     * 완료 체크(log = null)는 바로 백업한다 — 서비스의 주기 저장은 시간만 쓰기 때문 (#668)
     * 내용 수정(log != null)은 이어서 부르는 onGoalInputDialogChange(false)가 저장하므로 여기서는 저장하지 않는다
     */
    fun onStudyLogChanged(index: Int, completed: Boolean? = null, log: String? = null) {
        updateStudyLog(index, completed, log)
        if (log == null) viewModelScope.launch { saveSession() }
    }

    private fun updateStudyLog(index: Int, completed: Boolean?, log: String?) = _uiState.update {
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
     * "공부 중" 표시(studying/{uid}) 즉시 삭제 — 오프라인이면 연결 후 반영되므로 완료를 기다리지 않음 (HomeViewModel과 같은 방식)
     * 삭제 요청은 대기열에 들어간 뒤에는 화면을 떠나 코루틴이 취소돼도 전송된다
     * 일시정지로 걸어 둔 예약이 있으면 취소한다 — 종료·에러 확인·저장할 수 없는 세션은 기다리지 않고 바로 지움 (#667)
     * 종료 저장(onFinishStudyingClick)은 IO 스레드에서 부르므로, 예약 취소도 삭제 요청과 함께 viewModelScope(메인) 안에서 한다
     * — 메인에서 부르면 Main.immediate라 바로 실행되고, IO에서 부르면 메인으로 넘어간 뒤 실행된다
     * 실패해도 저장 결과에는 영향이 없어 별도 처리 없음 (에러 로그는 레포지토리에서 찍고 있음)
     */
    private fun deleteStudyingUserInfo() {
        viewModelScope.launch {
            cancelScheduledStudyingUserDelete()
            repository.deleteStudyingUserInfo()
        }
    }

    /**
     * 일시정지 후 STUDYING_USER_DELETE_DELAY_MS 뒤에 "공부 중" 표시 삭제 (#667)
     * 그 안에 재개하면 startStopwatch가 취소해 문서를 지우지 않고 갱신만 한다
     * 연타로 다시 예약되면 이전 예약을 취소해 예약이 하나만 남게 한다
     * 대기 중에 학습 화면이 안 보이게 되면(홈 버튼·화면 끄기·최근 앱 목록에서 닫기) onScreenStopped가 바로 지운다
     * 대기 중에 ON_STOP 없이 학습 화면이 사라지거나(화면이 보이는 채로 프로세스가 끝남(충돌 등)),
     * 세션 만료로 로그인 화면으로 이동하면(로그아웃 뒤라 ON_STOP의 삭제가 uid가 없어 실패) 예약도 함께 취소되어
     * 서버의 오래된 문서 정리(cleanUpStaleStudying)가 지운다
     * - onCleared에서 따로 지우지 않는다: 앱 종료는 onCleared 직후 프로세스가 끝나 요청이 서버에 닿지 못하고
     *   (에뮬레이터에서 0.05~0.12초 뒤 종료, 2회 모두 상대 화면에 남음), 세션 만료는 로그아웃(MainActivity) 뒤에
     *   화면이 정리돼 uid가 없어 요청이 실패한다
     */
    private fun scheduleStudyingUserDelete() {
        studyingUserDeleteJob?.cancel()
        studyingUserDeleteJob = viewModelScope.launch {
            delay(STUDYING_USER_DELETE_DELAY_MS)
            repository.deleteStudyingUserInfo()
        }
    }

    private fun cancelScheduledStudyingUserDelete() {
        studyingUserDeleteJob?.cancel()
        studyingUserDeleteJob = null
    }

    /**
     * 학습 화면이 안 보이게 됨(ON_STOP) — 일시정지 삭제 예약이 대기 중이면 기다리지 않고 바로 지운다 (#667)
     * 백그라운드로 가면 몇 초 뒤 앱 네트워크가 막혀(Android 16 에뮬레이터에서 onStop 약 5초 뒤 차단 확인)
     * 10초 뒤 삭제가 대기열에만 남고 상대 화면에 계속 보이기 때문
     * - 예약한 삭제가 이미 실행된 Job도 변수에 남아 있으므로 null이 아닌 isActive로 확인 (끝난 뒤 매번 다시 지우지 않게)
     * - 공부 중(예약 없음)·종료 후 결과 화면 이동(종료 때 이미 취소)은 삭제하지 않음
     *
     * 로컬 백업도 바로 저장한다 — 시스템이 앱을 끄는 건 대부분 백그라운드에서라, 주기(LOCAL_BACKUP_INTERVAL_MS)를 기다리지 않게 (#668)
     * - 다른 화면으로 이동할 때도 ON_STOP이 오므로 저장하면 안 되는 경우를 거른다
     *   - 초기 로딩 전(isLoading): 백업 복원 전이라 새 세션 값으로 덮게 됨. 종료 세션이라 홈으로 보낼 때도 loaded()를 부르지 않아 여기서 걸림
     *   - 종료 확정 후(isFinished): saveSession이 STUDYING 저장을 건너뜀 (결과 화면 이동·에러 확인·저장할 수 없는 세션)
     */
    fun onScreenStopped() {
        if (studyingUserDeleteJob?.isActive == true) deleteStudyingUserInfo()
        if (!isLoading.value) viewModelScope.launch { saveSession() }
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

    /**
     * 화면이 알림 상태를 확인한 결과 (#610) — 알림이 안 보여도 타이머·저장은 그대로 진행한다
     * - 첫 거절: 다음 진입 때 시스템이 다시 물으므로 짧게 이유만 알린다
     * - 요청 창으로 켤 수 없다고 판단(BLOCKED): 하루 1번만 설정 화면 안내 (같은 날 재진입·일시정지·복구 후에는 띄우지 않음)
     */
    fun onNotificationAccessChecked(access: NotificationAccess) {
        when (access) {
            NotificationAccess.ALLOWED -> Unit
            NotificationAccess.DENIED_CAN_ASK_AGAIN -> sendToast(NOTIFICATION_DENIED_MESSAGE, Toast.LENGTH_LONG)
            NotificationAccess.BLOCKED -> viewModelScope.launch {
                if (claimDailyNotificationGuideUseCase()) {
                    _uiState.update { it.copy(isNotificationGuideShown = true) }
                }
            }
        }
    }

    fun onNotificationGuideDismiss() {
        _uiState.update { it.copy(isNotificationGuideShown = false) }
    }

    /** 설정에서 돌아와도 다시 확인하지 않으므로, 허용하지 않고 돌아와도 안내가 바로 다시 뜨지 않는다 */
    fun onNotificationGuideSettingsClick() {
        _uiState.update { it.copy(isNotificationGuideShown = false) }
        viewModelScope.launch { _eventChannel.send(StudyingEvent.OpenNotificationSettings) }
    }

    private fun sendToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        viewModelScope.launch { _eventChannel.send(StudyingEvent.ShowToast(message, duration)) }
    }

    companion object {
        // 마지막 입력 후 임시 저장까지 기다리는 시간
        private const val GOAL_DRAFT_SAVE_DELAY_MS = 500L
        // 일시정지 후 "공부 중" 표시를 지우기까지 기다리는 시간 (#667)
        private const val STUDYING_USER_DELETE_DELAY_MS = 10_000L
        private const val SAVE_PENDING_MESSAGE = "네트워크에 연결되면 학습 기록이 자동으로 저장돼요."
        private const val NOTIFICATION_DENIED_MESSAGE = "알림을 허용하면 다른 앱을 쓰는 중에도 학습 시간을 볼 수 있어요."
    }

}
