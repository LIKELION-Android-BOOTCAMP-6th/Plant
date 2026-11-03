package com.a32b.plant.presentation.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.model.DailyCheckThisMonth
import com.a32b.plant.domain.model.Item
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.usecase.item.OpenBonusBoxUseCase
import com.a32b.plant.domain.usecase.pot.GetActivePotUseCase
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import com.a32b.plant.domain.usecase.pot.GetTodayTotalStudyTimeUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.usecase.studying.GetRecoverySessionUseCase
import com.a32b.plant.domain.usecase.studying.RecoverStudySessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AttendanceUiState(
    val count: Int = 0,
    val buttonText: String = "출석하기",
    val isAttendanceCompleted: Boolean = false,
    val isChecking: Boolean = false,
    val reward: AttendanceReward? = null
)

sealed class HomeEvent {
    data class ShowToast(val message: String) : HomeEvent()
    data class BonusBoxResult(val items: List<ItemType>) : HomeEvent()
}

private fun DailyCheckThisMonth.toAttendanceUiState(): AttendanceUiState {
    val nowKst = LocalDate.now(ZoneId.of("Asia/Seoul"))
    val decision = decideNext(nowKst)

    // newCount == 1은 "이번이 이번 달 첫 체크가 될 것"이라는 뜻(신규 달 진입 포함).
    // 그 외에는 저장된 count를 쓰되, 손상된 값(28 초과)이 그대로 화면에 새지 않게 clamp한다.
    val displayCount = if (decision is AttendanceDecision.Success && decision.newCount == 1) {
        0
    } else {
        count.coerceIn(0, 28)
    }

    return when (decision) {
        is AttendanceDecision.Success ->
            AttendanceUiState(displayCount, "출석하기")

        AttendanceDecision.AlreadyChecked ->
            AttendanceUiState(
                count = displayCount,
                buttonText = "오늘 출석 완료",
                isAttendanceCompleted = true
            )

        AttendanceDecision.MonthCompleted ->
            AttendanceUiState(
                count = displayCount,
                buttonText = "이번 달 출석 완료",
                isAttendanceCompleted = true
            )
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActivePotUseCase: GetActivePotUseCase,
    private val getPotListUseCase: GetPotListUseCase,
    private val userRepository: UserRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val getTodayTotalStudyTimeUseCase: GetTodayTotalStudyTimeUseCase,
    private val studyingRepository: StudyingRepository,
    private val getRecoverySessionUseCase: GetRecoverySessionUseCase,
    private val recoverStudySessionUseCase: RecoverStudySessionUseCase,
    private val openBonusBoxUseCase: OpenBonusBoxUseCase
) : BaseViewModel() {

    private val _displayPot = MutableStateFlow(Pot.EMPTY)
    val displayPot = _displayPot.asStateFlow()
    //팝업 제어용
    private val _showPotChangeDialog = MutableStateFlow(false)
    val showPotChangeDialog = _showPotChangeDialog.asStateFlow()

    // 팝업 리스트
    private val _allPots = MutableStateFlow<List<Pot>>(emptyList())
    val allPots = _allPots.asStateFlow()

    //팝업 임시 선택
    private val _tempSelectedPot = MutableStateFlow<Pot?>(null)
    val tempSelectedPot = _tempSelectedPot.asStateFlow()

    //유저 이름 상태
    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    // 출석체크 UI 상태
    private val _attendanceUiState = MutableStateFlow(AttendanceUiState())
    val attendanceUiState = _attendanceUiState.asStateFlow()

    // 일회성 화면 이벤트
    private val _eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    //오늘 학습 시간 상태
    private val _todayStudyTime = MutableStateFlow("00:00:00")
    val todayStudyTime = _todayStudyTime.asStateFlow()


    //아이템 상태
    private val _userItem = MutableStateFlow(Item(0, 0, 0, 0, 0, 0))
    val userItem = _userItem.asStateFlow()

    // 복구 세션 상태
    private val _recoverySession = MutableStateFlow<StudyingSession?>(null)
    val recoverySession = _recoverySession.asStateFlow()

    // 복구 처리 중 로딩
    private val _isRecovering = MutableStateFlow(false)
    val isRecovering = _isRecovering.asStateFlow()

    // 진행 중인 복구 세션 확인 — 저장·폐기를 시작하면 취소해 늦게 끝난 확인이 지운 세션을 되살리지 않게 함
    private var recoveryCheckJob: Job? = null

    //박스 개봉 로딩
    private val _isBoxOpening = MutableStateFlow(false)
    val isBoxOpening = _isBoxOpening.asStateFlow()

    init {
        checkUserAndLoadData()
        observeUserItem()
    }
    private fun checkUserAndLoadData() {
        viewModelScope.launch {
            ensureCurrentUserUseCase { user ->
                _userName.value = user.nickname
                _attendanceUiState.value = user.monthCheck.toAttendanceUiState()
                _userItem.value = user.item

                loadActivePot(uid = user.uid, lastSelectedPotId = user.lastSelectedPotId)
            } ?: Unit
            loaded()
        }
    }


    private fun observeUserItem() {
        viewModelScope.launch {
            userRepository.currentUser.collect { currentUser ->
                currentUser?.let {
                    _userItem.value = it.item
                    _userName.value = it.nickname

                    val newState = it.monthCheck.toAttendanceUiState()
                    _attendanceUiState.update { currentUiState ->
                        newState.copy(reward = currentUiState.reward)
                    }                }
            }
        }
    }

    /**
     * 저장되지 않은 학습 세션 확인
     * 결과 화면에서 돌아올 때는 ViewModel이 유지되므로, 홈이 다시 보일 때마다(ON_RESUME) 호출
     * 복구 처리 중에는 지워지기 전의 백업을 다시 읽어 창이 되살아나지 않도록 건너뜀
     */
    fun checkRecoverySession() {
        if (_isRecovering.value) return
        recoveryCheckJob = viewModelScope.launch {
            getRecoverySessionUseCase()
                .onSuccess { session -> _recoverySession.value = session }
        }
    }

    /** 저장에 실패하거나 제한 시간이 지나면(AppError.Network) 창을 유지해 다시 선택할 수 있게 함 */
    fun recoverSession() {
        val session = _recoverySession.value ?: return
        if (_isRecovering.value) return
        recoveryCheckJob?.cancel()
        _isRecovering.value = true
        viewModelScope.launch {
            recoverStudySessionUseCase(session)
                .onSuccess {
                    _recoverySession.value = null
                    sendToast("학습 기록이 저장되었습니다.")
                    deleteStudyingUserInfo()
                }
                .onFailure { error ->
                    when (error) {
                        is AppError.UnknownUser -> ensureCurrentUserUseCase()
                        else -> sendToast(error.message)
                    }
                }
            _isRecovering.value = false
        }
    }

    /** 로컬 백업만 지우면 바로 끝나므로 네트워크 상태와 상관없이 창을 닫음 */
    fun discardSession() {
        if (_isRecovering.value) return
        recoveryCheckJob?.cancel()
        _isRecovering.value = true
        viewModelScope.launch {
            studyingRepository.clearLocalSession()
                .onSuccess {
                    _recoverySession.value = null
                    deleteStudyingUserInfo()
                }
                .onFailure { error -> sendToast(error.message) }
            _isRecovering.value = false
        }
    }

    /**
     * "공부 중" 표시(studying/{uid}) 삭제 — 오프라인이면 연결 후 반영되므로 완료를 기다리지 않음
     * 실패해도 복구·폐기 결과에는 영향이 없어 별도 처리 없음 (에러 로그는 레포지토리에서 찍고 있음)
     */
    private fun deleteStudyingUserInfo() {
        viewModelScope.launch { studyingRepository.deleteStudyingUserInfo() }
    }

    private fun loadActivePot(uid: String, lastSelectedPotId: String){
        viewModelScope.launch {
            safeRunCatching {
                getActivePotUseCase(uid = uid, lastSelectedPotId = lastSelectedPotId)
                    .collect { pot ->
                        _displayPot.value = pot
                        if (pot.id.isNotEmpty() && pot != Pot.EMPTY) {
                            fetchTodayStudyTime(uid, pot.id)
                        } else {
                            _todayStudyTime.value = "00:00:00"
                        }
                    }
            }.onFailure { throwable ->
                // 필요시 에러 핸들링
            }
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch {
            _eventChannel.send(HomeEvent.ShowToast(message))
        }
    }

    private fun fetchTodayStudyTime(uid: String, potId: String) {
        viewModelScope.launch {
            getTodayTotalStudyTimeUseCase(uid, potId)
                .onSuccess { totalMillis ->
                    _todayStudyTime.value = TimeFormatter.formatToDigitalClock(totalMillis)
                }
                .onFailure { error ->
                    _todayStudyTime.value = "00:00:00"
                    sendToast(error.message)
                }
        }
    }

    fun setTempSelectedPot(pot: Pot) {
        _tempSelectedPot.value = pot
    }

    fun confirmPotChange() {
        _tempSelectedPot.value?.let { selected ->
            _displayPot.value = selected

            viewModelScope.launch {
                safeRunCatching {
                    ensureCurrentUserUseCase { user ->
                        userRepository.updateLastSelectedPot(user.uid, selected.id)
                        fetchTodayStudyTime(user.uid, selected.id)
                    }
                }.onFailure {
                    // 예외 처리
                }
            }
        }
        setShowPotChangeDialog(false)
    }

    fun setShowPotChangeDialog(show: Boolean) {
        if (!show) _tempSelectedPot.value = null // 닫을 때 초기화
        else {
            _tempSelectedPot.value = _displayPot.value
            fetchStudyingPotsForDialog()
        }

        _showPotChangeDialog.value = show
    }

    private fun fetchStudyingPotsForDialog() {
        viewModelScope.launch {
            safeRunCatching {
                ensureCurrentUserUseCase { user ->
                    val studyingPots = getPotListUseCase.getStudyingPots(uid = user.uid).first()
                    _allPots.value = studyingPots
                }
            }.onFailure { throwable ->

            }
        }
    }

    fun checkAttendance() {
        if (_attendanceUiState.value.isChecking) return

        viewModelScope.launch {
            _attendanceUiState.update { it.copy(isChecking = true) }

            try {
                ensureCurrentUserUseCase { user ->
                    userRepository.checkAttendance(user.uid)
                        .onSuccess { decision ->
                            _attendanceUiState.update {
                                it.copy(
                                    count = decision.newCount,
                                    buttonText = if (decision.isMonthCompleted) {
                                        "이번 달 출석 완료"
                                    } else {
                                        "오늘 출석 완료"
                                    },
                                    isAttendanceCompleted = true,
                                    reward = decision.reward
                                )
                            }
                        }
                        .onFailure { error ->
                            when (error) {
                                is AppError.UnknownUser -> ensureCurrentUserUseCase()
                                else -> _eventChannel.send(HomeEvent.ShowToast(error.message))
                            }
                        }
                }
            } finally {
                _attendanceUiState.update { it.copy(isChecking = false) }
            }
        }
    }

    fun dismissAttendanceReward() {
        _attendanceUiState.update { it.copy(reward = null) }
    }

    fun openBoxes(count: Int) {
        if (count <= 0) return
        if (_isBoxOpening.value) return

        viewModelScope.launch {
            _isBoxOpening.value = true
            try {
                openBonusBoxUseCase(count)
                    .onSuccess { droppedItem ->
                        if (droppedItem.isNotEmpty()) {
                            _eventChannel.send(HomeEvent.BonusBoxResult(droppedItem))
                        }
                    }
                    .onFailure { error ->
                        when (error) {
                            is AppError.UnknownUser -> ensureCurrentUserUseCase()
                            else -> sendToast(error.message)
                        }
                    }
            } finally {
                _isBoxOpening.value = false
            }
        }
    }
}
