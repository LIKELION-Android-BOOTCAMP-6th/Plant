package com.a32b.plant.ui.feature.studying.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.data.local.StudyingSession
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.data.model.StudyingUser
import com.a32b.plant.data.repository.PotRepository
import com.a32b.plant.data.repository.StudyingRepository
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

data class StudyingUiState(
    val tag: String,
    val timer: Long = 0L,
    val isStudying: Boolean = true, //스톱워치 가동을 위한 공부중 여부 체크
    val buttonText: String = "일시정지",
    val studyingUsers: List<StudyingUser> = emptyList(),
    val isFinishDialogShown: Boolean = false, //학습 종료 다이얼로그 표출 여부 체크
    val studyLog: List<String> = emptyList(),
    val isStudyFinish: Boolean = false, //true시 학습 완전 종료, 디비로 값 넘기기
    val isInterruptedSession: Boolean = false, //비정상 종료 여부 체크
    val interruptedStudyLog: StudyingSession? = null
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
}
class StudyingViewModel(
    private val repository: StudyingRepository,
    private val potRepository: PotRepository,
    private val tag: String,
    private val potId: String,
    private val title: String,
    private val startTime: String,
    private val level: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyingUiState(tag = tag))
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<StudyingEvent>(Channel.BUFFERED)
    val event = _eventChannel.receiveAsFlow()

    /** 비정상 종료 대비 로컬 디비에 데이터 저장   */

    fun saveSession(){
        viewModelScope.launch(Dispatchers.IO) {
            while (_uiState.value.isStudying){
                delay(5000L)
                repository.saveSession(StudyingSession(CurrentUser.uid, tag, title, potId, _uiState.value.timer))
            }
        }
    }

    /** db에서 같은 태그로 공부중인 사용자 데이터 가져오기 */
    fun onStudyingUsersChange(){
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(studyingUsers = repository.getStudyingUser(tag)) }
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
//                if(_uiState.value.timer % 600000L == 0L){
                if(_uiState.value.timer % 6000L == 0L){
                    updateUser()
                    onStudyingUsersChange()
                }
            }
        }
    }

    fun updateUser(){
        viewModelScope.launch(Dispatchers.IO) {
            repository.updateStudyingUser(
                StudyingUser(CurrentUser.uid, CurrentUser.nickname, CurrentUser.profileImg, tag, _uiState.value.timer)
            )
        }

    }
    fun stopStopwatch(){
        _uiState.update { it.copy(isStudying = false) }
        job?.cancel()
    }

    init {
        startStopwatch()
        saveSession()
        updateUser()
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
        val timestamp = "${TimeFormatter.formatToKoreanDate(LocalDateTime.now())} $startTime ~ ${getCurrentTime()}"
        val resultTimestamp = "${TimeFormatter.formatWithDayOfWeek(LocalDateTime.now())} $startTime ~ ${getCurrentTime()}"
        fun setStudyLog(): StudyLog = StudyLog(timestamp, _uiState.value.studyLog, _uiState.value.timer)
        potRepository.createStudyLog(potId, setStudyLog())
        potRepository.updateTotalStudyTime(potId, _uiState.value.timer)
        repository.updateUserTotalStudyTime(_uiState.value.timer)
        repository.deleteStudyingUser()

        viewModelScope.launch{
            //종료 시 로컬디비에 저장된 데이터 삭제
            withContext(Dispatchers.IO) {
                repository.clearSession()
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





}