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
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.usecase.studying.ClearStudyingSessionUseCase
import com.a32b.plant.domain.usecase.studying.FinishStudyingUseCase
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

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
    val error: String? = null,
    val isGoalInputDialogShown: Boolean = true, //학습 목표 입력
    val goalCheckMode: StudyingGoalCheckMode? = null,
    val wasStudyingBeforeGoalInputDialog: Boolean = true,
    val wasStudyingBeforeGoalCheckDialog: Boolean = false,
    val isEditing: Boolean = false,
    val logIndex: Int? = null,
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
        val level: String

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
    private val clearStudyingSessionUseCase: ClearStudyingSessionUseCase,
    private val finishStudyingUseCase: FinishStudyingUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
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

    init {
        viewModelScope.launch {
            restoreFromLocalBackup()
            // 로컬 복구 결과(타이머·목표 다이얼로그 표시 여부)가 확정된 뒤 화면 노출
            // 원격 등록(initStudyingUser)은 화면 표시와 무관하므로 기다리지 않음
            loaded()
            initStudyingUser()
        }
        onStudyingUsersChange()
        observeTimer()
    }

    private suspend fun restoreFromLocalBackup() {
        repository.readLocalSession()
            .onSuccess { session ->
                if (session != null && session.potId == potId) {
                    lastSaveMs = session.time
                    lastUpdateMs = session.time
                    _uiState.update {
                        it.copy(
                            timer = session.time,
                            startedAt = session.startedAt,
                            studyLog = session.log?.map { log -> StudyLogUi(log = log) }?.filter { it.log.isNotBlank() } ?: emptyList(),
                            isGoalInputDialogShown = false,
                            isStudying = true
                        )
                    }
                    startStopwatch()
                }
            }
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

    /** 현재 시간 기록 */
    fun onStartedAtChange(value : Long) = _uiState.update { it.copy(startedAt = value) }

    /** 비정상 종료 대비 로컬 디비에 데이터 저장   */
    private suspend fun saveSession(){
        updateLocalStudyingSessionUseCase(_uiState.value.tag, _uiState.value.title, potId, _uiState.value.timer, _uiState.value.studyLog.map { it.log }, startedAt = _uiState.value.startedAt)
            .onSuccess { _uiState.update { it.copy(isLocalSaved = true) } }
            .onFailure { e ->
                if (e is AppError.Custom) _uiState.update { it.copy(isLocalSaved = false) }
            }
    }

    /** 최초 시작 시 로컬 + 원격 db에 현재 사용자 정보 저장 */
    suspend fun initStudyingUser(){
        withContext(Dispatchers.IO){
            startStudyingSessionUseCase(_uiState.value.tag, _uiState.value.title,potId,_uiState.value.timer, _uiState.value.studyLog.map { it.log }, startedAt = _uiState.value.startedAt)
                .onFailure { e ->
                    when (e){
                        is AppError.UnknownUser -> Unit //유즈케이스에서 호출했으므로 따로 호출 x
                        is AppError.Local, is AppError.Custom -> Unit //에러 로그는 레포지토리에서 찍고 있음
                        is AppError.Network -> sendToast(e.message)
                        else -> _uiState.update { it.copy(error = e.message) }
                    }
                }
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

    /** [DEBUG] 타이머에 1시간(3,600,000ms) 추가 — 보상 테스트용, 릴리즈 전 제거 */
    fun addDebugTime() {
        val newTimer = _uiState.value.timer + 3_600_000L
        lastSaveMs = newTimer
        lastUpdateMs = newTimer
        if (_uiState.value.isStudying) {
            sendServiceIntent(StudyingTimerService.ACTION_START) {
                putExtra(StudyingTimerService.EXTRA_ACCUMULATED, newTimer)
            }
        }
    }

    private fun startStopwatch() {
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
        viewModelScope.launch {
            updateLocalStudyingSessionUseCase(_uiState.value.tag, _uiState.value.title, potId, _uiState.value.timer, _uiState.value.studyLog.map { it.log }, startedAt = _uiState.value.startedAt)
        }
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
            newState.copy(isGoalInputDialogShown = true, goalCheckMode = null)
        }
    }

    /** 학습 목표 설정 다이얼로그 표출 여부 제어*/
    fun onGoalInputDialogChange(value: Boolean) {
        if (!_uiState.value.isEditing) {
            if (value) {
                val wasStudying = _uiState.value.isStudying
                _uiState.update { it.copy(isGoalInputDialogShown = true, wasStudyingBeforeGoalInputDialog = wasStudying) }
                onStudyingStatusChange(false)
            } else {
                val restore = _uiState.value.wasStudyingBeforeGoalInputDialog
                _uiState.update { it.copy(isGoalInputDialogShown = false) }
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
                    goalCheckMode = savedGoalCheckMode
                )}
                savedGoalCheckMode = null
                finishEditing()
            }
        }
    }

    /** 학습 목표 확인 다이얼로그 표출 모드 제어*/
    fun onChangeGoalCheckMode(mode: StudyingGoalCheckMode?) {
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
        sendServiceIntent(StudyingTimerService.ACTION_STOP)
        _uiState.update { it.copy(isLoading = true, goalCheckMode = null) }
        onFinishStudyingClick()
    }

    private fun getCurrentTime(): String{
        val now = LocalDateTime.now()
        return TimeFormatter.formatToTimeOnly(now)
    }

    /** 학습 내역 저장 및 결과창으로 이동 */
    private fun onFinishStudyingClick() {
        var isLogSaved = true
        val startTime = TimeFormatter.formatToTimeOnly(_uiState.value.startedAt)
        //개별 학습 기록의 제목
        val timestamp = "${TimeFormatter.formatToKoreanDate(LocalDateTime.now())} ${startTime} ~ ${getCurrentTime()}"
        val resultTimestamp = "${TimeFormatter.formatWithDayOfWeek(LocalDateTime.now())} ${startTime} ~ ${getCurrentTime()}"

        viewModelScope.launch(Dispatchers.IO){

            val result = withTimeoutOrNull(5.seconds){
                finishStudyingUseCase(
                    potId = potId,
                    timestamp = timestamp,
                    log = _uiState.value.studyLog.map { it.log },
                    completed = _uiState.value.studyLog.map { it.isCompleted },
                    time = _uiState.value.timer,
                    startedAt = _uiState.value.startedAt
                )
                    .onSuccess { clearSession() }
                    .onFailure { e ->
                        when (e){
                            is AppError.UnknownUser -> ensureCurrentUserUseCase()
                            is AppError.Network -> sendToast(e.message)
                            else -> {
                                isLogSaved = false
                                _uiState.update { it.copy(error = e.message) }
                            }
                        }
                    }
            }
            if (result == null) sendToast("네트워크 연결 상태를 확인해주세요.")

            _uiState.update { it.copy(isLoading = false) }

            if (isLogSaved){
                _eventChannel.send(StudyingEvent.NavigateToStudyResult(
                    timestamp = resultTimestamp,
                    tag = _uiState.value.tag,
                    potId = potId,
                    title = _uiState.value.title,
                    log = _uiState.value.studyLog.map { it.log },
                    completed = _uiState.value.studyLog.map { it.isCompleted },
                    time = _uiState.value.timer,
                    level = _uiState.value.level
                ))
            }

        }
    }
    private suspend fun clearSession(){
        clearStudyingSessionUseCase()
            .onFailure { e ->
                when (e){
                    is AppError.UnknownUser -> ensureCurrentUserUseCase()
                    is AppError.Local -> sendToast("자동 저장된 학습 기록이 지워지지 않았습니다.")
                    else -> Unit //디비에서 유저 정보 삭제 실패 시 유저에게 안내할 필요 x
                }
            }
    }

    /**  에러 다이얼로그 표출 시 세션 클리어 및 홈으로 이동 */
    fun onErrorConfirmClicked(){
        sendServiceIntent(StudyingTimerService.ACTION_STOP)
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            clearStudyingSessionUseCase(true)
            _uiState.update { it.copy(error = null, isLoading = false) }
            _eventChannel.send(StudyingEvent.NavigateToHome)
        }
    }

    private fun sendToast(message: String, duration: Int = Toast.LENGTH_SHORT) {
        viewModelScope.launch { _eventChannel.send(StudyingEvent.ShowToast(message, duration)) }
    }

}
