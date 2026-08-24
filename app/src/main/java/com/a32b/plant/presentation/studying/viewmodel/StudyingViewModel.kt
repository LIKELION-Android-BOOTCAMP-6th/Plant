package com.a32b.plant.presentation.studying.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDateTime
import javax.inject.Inject
import kotlin.math.log
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
    val isStudyFinish: Boolean = false, //true시 학습 완전 종료, 디비로 값 넘기기
    val isLocalSaved: Boolean = true, // 로컬 저장 성공 여부 체크
    val startTime: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
    val isGoalInputDialogShown: Boolean = true, //학습 목표 입력
    val goalCheckMode: StudyingGoalCheckMode? = null
)

sealed class StudyingEvent{
    data class NavigateToStudyResult(
        val timestamp: String, //날짜 시작시간 ~ 종료 시간
        val tag: String,
        val title: String,
        val log: List<String>,
        val time: Long,
        val potId: String,
        val level: String

        ): StudyingEvent()

    object NavigateToHome : StudyingEvent()
    data class ShowToast(val message: String) : StudyingEvent()
}
@HiltViewModel
class StudyingViewModel @Inject constructor(
    private val repository: StudyingRepository,
    private val startStudyingSessionUseCase: StartStudyingSessionUseCase,
    private val updateLocalStudyingSessionUseCase: UpdateLocalStudyingSessionUseCase,
    private val clearStudyingSessionUseCase: ClearStudyingSessionUseCase,
    private val finishStudyingUseCase: FinishStudyingUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    savedStateHandle: SavedStateHandle

) : ViewModel() {
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


    init {
        viewModelScope.launch {
            initStudyingUser()
        }
        onStudyingUsersChange()
    }
    /** 현재 시간 기록 */
    fun onStartTimeChange(value : String) = _uiState.update { it.copy(startTime = value) }

    /** 비정상 종료 대비 로컬 디비에 데이터 저장   */
    private suspend fun saveSession(){
        updateLocalStudyingSessionUseCase(_uiState.value.tag, _uiState.value.title, potId,_uiState.value.timer)
            .onSuccess { _uiState.update { it.copy(isLocalSaved = true) } }
            .onFailure { e ->
                if (e is AppError.Custom) _uiState.update { it.copy(isLocalSaved = false) }
            }
    }

    /** 최초 시작 시 로컬 + 원격 db에 현재 사용자 정보 저장 */
    suspend fun initStudyingUser(){
        withContext(Dispatchers.IO){
            startStudyingSessionUseCase(_uiState.value.tag, _uiState.value.title,potId,_uiState.value.timer, _uiState.value.studyLog.map { it.log })
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

    /** 스톱워치 */
    private var job: Job? = null
    fun onTimerChange() = _uiState.update { it.copy(timer = it.timer + 1000 ) }
    private fun startStopwatch(){
        job?.cancel()
        job = viewModelScope.launch {
            while (true){
                delay(1000)
                onTimerChange()
                if(_uiState.value.timer % 5_000L == 0L) saveSession() //5초마다 로컬 디비 저장
                if(_uiState.value.timer % 60_000L == 0L) updateUser() //1분마다 원격 디비 저장
            }
        }
    }
    private fun stopStopwatch(){
        _uiState.update { it.copy(isStudying = false) }
        job?.cancel()
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
        Log.d("입력값 확인", "$studyLog")
        viewModelScope.launch {
            updateLocalStudyingSessionUseCase(_uiState.value.tag, _uiState.value.title, potId, _uiState.value.timer, _uiState.value.studyLog.map { it.log })
        }
    }

    /** 학습 목표 다이얼로그 표출 여부 제어*/
    fun onGoalInputDialogChange(value: Boolean) {
        _uiState.update { it.copy(isGoalInputDialogShown = value) }
        onStudyingStatusChange(!value)
    }

    /** 학습 목표 확인 다이얼로그 표출 모드 제어*/
    fun onChangeGoalCheckMode(mode: StudyingGoalCheckMode?){
        _uiState.update { it.copy(goalCheckMode = mode) }
        if (mode != null) onStudyingStatusChange(false)
        else onStudyingStatusChange(true)
    }

    /** 목표 달성 토글 */
    fun onStudyLogCompleted(index: Int, value: Boolean) = _uiState.update {
        it.copy(studyLog = it.studyLog.mapIndexed { i, log ->
            if (i == index) log.copy(isCompleted = value) else log
        })
    }

    /** 학습 완전 종료 시 (= 다이얼로그에서도 기록 입력 후 종료 버튼 클릭했을 때)    */
    fun onIsStudyFinishChange() = _uiState.update { it.copy(isStudyFinish = true) }

    private fun getCurrentTime(): String{
        val now = LocalDateTime.now()
        return TimeFormatter.formatToTimeOnly(now)
    }

    /** 학습 내역 저장 및 결과창으로 이동 */
    fun onFinishStudyingClick() {
        var isLogSaved = true
        _uiState.update { it.copy(isLoading = true) }
        //개별 학습 기록의 제목
        val timestamp = "${TimeFormatter.formatToKoreanDate(LocalDateTime.now())} ${_uiState.value.startTime} ~ ${getCurrentTime()}"
        val resultTimestamp = "${TimeFormatter.formatWithDayOfWeek(LocalDateTime.now())} ${_uiState.value.startTime} ~ ${getCurrentTime()}"

        viewModelScope.launch(Dispatchers.IO){

            val result = withTimeoutOrNull(5.seconds){
                finishStudyingUseCase(potId, timestamp, _uiState.value.studyLog.map { it.log }, _uiState.value.timer)
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
                    log = _uiState.value.studyLog.map { it.log } ?: emptyList(),
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
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            clearStudyingSessionUseCase(true)
            _uiState.update { it.copy(error = null, isLoading = false) }
            _eventChannel.send(StudyingEvent.NavigateToHome)
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(StudyingEvent.ShowToast(message)) }
    }

}
