package com.a32b.plant.ui.feature.studying.viewmodel

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.local.StudyingSession
import com.a32b.plant.data.model.StudyLog
import com.a32b.plant.data.model.StudyingUser
import com.a32b.plant.data.repository.PotRepository
import com.a32b.plant.data.repository.StudyingRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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
        viewModelScope.launch {
            while (_uiState.value.isStudying){
                delay(5000L)
                repository.saveSession(StudyingSession(CurrentUser.uid, tag, title, potId, _uiState.value.timer))
            }
        }
    }

    /*
    1. 데이터스토어에서 불러와
    2. 빈 값이 아니면 저장되어 있는 유아이디랑 현재 사용자의 유아이디가 같은지 확인해
    2-1. 같다면 이전 기록이 있다고 말하고, 이전 기록으로 학습을 이어갈건지 물어보는 다이얼로그를 띄워
    2-2. 이어간다고 하면 -> 그 값으로 세팅해
    2-3. 안 이어간다고 하면 걍 다이얼로그 닫고 끝내기
    ⭐내일 와서 어플리케이션 파일 만들고 context 추가하기...
     */


    /** db에서 같은 태그로 공부중인 사용자 데이터 가져오기 */
    fun onStudyingUsersChange(){
        viewModelScope.launch {
            _uiState.update { it.copy(studyingUsers = repository.getStudyingUser(tag)) }
        }
    }

    /** 공부중 상태 변경 */
    fun onStudyingStatusChange() {
         _uiState.update { it.copy(isStudying = !it.isStudying) }

        if(_uiState.value.isStudying) startStopwatch()
        else stopStopwatch()
    }
//data store <- sharedPreference 상위호환 느낌
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
                    repository.updateStudyingUser(
                        StudyingUser(CurrentUser.uid, CurrentUser.nickname, CurrentUser.profileImg, tag, _uiState.value.timer)
//                        StudyingUser("zz", "zz", "lv.2", tag, _uiState.value.timer)
                    )
                    onStudyingUsersChange()
                }
            }
        }
    }
    fun stopStopwatch(){
        _uiState.update { it.copy(isStudying = false) }
        job?.cancel()
    }

    init {
        startStopwatch()
        saveSession()
    }

    /**  학습 종료 버튼 클릭 시 학습 기록하는 다이얼로그 표출    */
    fun onFinishDialogShownChange() = _uiState.update { it.copy(isFinishDialogShown = !it.isFinishDialogShown) }

    fun setStudyLog(log: List<String>) = _uiState.update { it.copy(studyLog = log) }

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
        fun setStudyLog(): StudyLog = StudyLog(timestamp, _uiState.value.studyLog, _uiState.value.timer)
        potRepository.createStudyLog(potId, setStudyLog())
        potRepository.updateTotalStudyTime(potId, _uiState.value.timer)
        repository.deleteStudyingUser()

        viewModelScope.launch {
            //종료 시 로컬디비에 저장된 데이터 삭제
            repository.clearSession()

            _eventChannel.send(StudyingEvent.NavigateToStudyResult(
                timestamp = timestamp,
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