package com.a32b.plant.presentation.studying.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyingUser
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.studying.StartStudyingSessionUseCase
import com.a32b.plant.domain.usecase.studying.UpdateLocalStudyingSessionUseCase
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
import java.time.LocalDateTime
import javax.inject.Inject

data class StudyingUiState(
    val tag: String,
    val timer: Long = 0L,
    val isStudying: Boolean = true, //스톱워치 가동을 위한 공부중 여부 체크
    val buttonText: String = "일시정지",
    val studyingUsers: List<StudyingUser> = emptyList(),
    val isFinishDialogShown: Boolean = false, //학습 종료 다이얼로그 표출 여부 체크
    val studyLog: List<String> = emptyList(),
    val isStudyFinish: Boolean = false, //true시 학습 완전 종료, 디비로 값 넘기기
    val isLocalSaved: Boolean = true, // 로컬 저장 성공 여부 체크
    val startTime: String = "",
    val isLoading: Boolean = false,
    val error: String? = null,
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
    savedStateHandle: SavedStateHandle

) : ViewModel() {
    private val _eventChannel = Channel<StudyingEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    private val tag: String = savedStateHandle["tagName"] ?: ""
    private val title: String = savedStateHandle["title"] ?: ""
    private val potId: String = savedStateHandle["potId"] ?: ""
    private val level: String = savedStateHandle["level"] ?: ""

    private val _uiState = MutableStateFlow(StudyingUiState(tag = tag))
    val uiState = _uiState.asStateFlow()



    /** 비정상 종료 대비 로컬 디비에 데이터 저장   */
    private suspend fun saveSession(){
        updateLocalStudyingSessionUseCase(tag, title, potId,_uiState.value.timer)
            .onSuccess { _uiState.update { it.copy(isLocalSaved = true) } }
            .onFailure { e ->
                if (e is AppError.Custom) _uiState.update { it.copy(isLocalSaved = false) }
            }
    }

    /** db에서 같은 태그로 공부중인 사용자 데이터 가져오기 */
    fun onStudyingUsersChange(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.observeStudyingUser(tag)
                .collect { users ->
                    _uiState.update { it.copy(studyingUsers = users) }
                }
        }
    }

    /** 공부중 상태 변경 */
    fun onStudyingStatusChange() {
         _uiState.update { it.copy(isStudying = !it.isStudying) }

        if(_uiState.value.isStudying) startStopwatch()
        else stopStopwatch()
    }

    /** 스톱워치 */
    private var job: Job? = null
    fun onTimerChange() = _uiState.update { it.copy(timer = it.timer + 1000 ) }
    fun startStopwatch(){
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
    private fun updateUser(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStudyingTime(tag, _uiState.value.timer)
        }

    }
    fun stopStopwatch(){
        _uiState.update { it.copy(isStudying = false) }
        job?.cancel()
    }

    init {
        startStopwatch()
        viewModelScope.launch {
            initStudyingUser()
        }
        onStudyingUsersChange()
    }

    /** 최초 시작 시 로컬 + 원격 db에 현재 사용자 정보 저장 */
    suspend fun initStudyingUser(){
        withContext(Dispatchers.IO){
            startStudyingSessionUseCase(tag, title,potId,_uiState.value.timer)
                .onFailure { e ->
                    when (e){
                        //TODO 유저 정보 없을 떄 에러 처리하기
                        is AppError.UnknownUser -> ""
                        is AppError.Local, is AppError.Custom -> Log.e("Studying", "로컬 저장 실패", e)
                        is AppError.Network -> sendToast(e.message)
                        else -> _uiState.update { it.copy(error = e.message) }
                    }


                }
        }
    }

    /**  학습 종료 버튼 클릭 시 학습 기록하는 다이얼로그 표출    */
    fun onFinishDialogShownChange() = _uiState.update { it.copy(isFinishDialogShown = !it.isFinishDialogShown) }

    fun setStudyLog(studyLog: List<String>) = _uiState.update { it.copy(studyLog = studyLog.filter { log -> log.isNotBlank()  }) }
    fun onDialogDismissClick(){
        _uiState.update { it.copy(isFinishDialogShown = false, isStudying = true) }
        startStopwatch()
    }

    /** 학습 완전 종료 시 (= 다이얼로그에서도 기록 입력 후 종료 버튼 클릭했을 때)    */
    fun onIsStudyFinishChange() = _uiState.update { it.copy(isStudyFinish = true) }
    fun getCurrentTime(): String{
        val now = LocalDateTime.now()
        return TimeFormatter.formatToTimeOnly(now)
    }
    fun onFinishStudyingClick() {

        //개별 학습 기록의 제목
        val timestamp = "${TimeFormatter.formatToKoreanDate(LocalDateTime.now())} ${_uiState.value.startTime} ~ ${getCurrentTime()}"
        val resultTimestamp = "${TimeFormatter.formatWithDayOfWeek(LocalDateTime.now())} ${_uiState.value.startTime} ~ ${getCurrentTime()}"
        fun setStudyLog(): StudyLog = StudyLog.write(timestamp, _uiState.value.studyLog, _uiState.value.timer)

        viewModelScope.launch{
            //종료 시 로컬디비에 저장된 데이터 삭제
            //TODO 실패 시 오류 알림 다이얼로그 띄우기
            withContext(Dispatchers.IO) {
                repository.saveStudyLog(potId, setStudyLog())
                repository.updateTotalStudyTime(potId, _uiState.value.timer)
                repository.updateUserTotalStudyTime(_uiState.value.timer)
                repository.deleteStudyingUserInfo()
                repository.clearLocalSession()
            }

            _eventChannel.send(StudyingEvent.NavigateToStudyResult(
                timestamp = resultTimestamp,
                tag = tag,
                potId = potId,
                title = title,
                log = _uiState.value.studyLog,
                time = _uiState.value.timer,
                level = level
            ))
        }
    }
    fun onStartTimeChange(value : String) = _uiState.update { it.copy(startTime = value) }

    private fun clearSession(){
        //TODO 유저 정보 삭제 유즈케이스
    }

    /**  에러 다이얼로그 표출 시 세션 클리어 및 홈으로 이동 */
    fun onErrorConfirmClicked(){
        clearSession()
        viewModelScope.launch {
            _eventChannel.send(StudyingEvent.NavigateToHome)
        }
        _uiState.update { it.copy(error = null) }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch { _eventChannel.send(StudyingEvent.ShowToast(message)) }
    }

}
