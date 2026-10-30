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
import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.usecase.item.OpenBonusBoxUseCase
import com.a32b.plant.domain.usecase.pot.CheckLevelUpConditionUseCase
import com.a32b.plant.domain.usecase.pot.GetActivePotUseCase
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import com.a32b.plant.domain.usecase.pot.GetTodayTotalStudyTimeUseCase
import com.a32b.plant.domain.usecase.pot.LevelUpPotUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.usecase.studying.ClearStudyingSessionUseCase
import com.a32b.plant.domain.usecase.studying.RecoverStudySessionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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

data class HomeUiState(
    val levelUpAvailable: Boolean = false, //레벨업 버튼 활성화
    val showInsufficientItemsDialog: Boolean = false, // 아이템 부족 다이얼로그
    val levelUpProgress: Float = 0f,
    val levelUpProgressText: String = "",
    val isLoading: Boolean = false
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
    private val recoverStudySessionUseCase: RecoverStudySessionUseCase,
    private val clearStudyingSessionUseCase: ClearStudyingSessionUseCase,
    private val openBonusBoxUseCase: OpenBonusBoxUseCase,
    private val checkLevelUpConditionUseCase: CheckLevelUpConditionUseCase,
    private val levelUpPotUseCase: LevelUpPotUseCase
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

    //박스 개봉 로딩
    private val _isBoxOpening = MutableStateFlow(false)
    val isBoxOpening = _isBoxOpening.asStateFlow()

    // 레벨업 상태
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

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
                checkRecoverySession(user.uid)
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
                    }
                    recheckLevelUpCondition()
                }
            }
        }
    }

    private suspend fun checkRecoverySession(uid: String) {
        studyingRepository.readLocalSession()
            .onSuccess { session ->
                if (session != null && session.userId == uid && session.time > 0L) {
                    _recoverySession.value = session
                }
            }
    }

    fun recoverSession() {
        val session = _recoverySession.value ?: return
        _isRecovering.value = true
        viewModelScope.launch {
            recoverStudySessionUseCase(session)
                .onSuccess {
                    _recoverySession.value = null
                    sendToast("학습 기록이 저장되었습니다.")
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

    fun discardSession() {
        _isRecovering.value = true
        viewModelScope.launch {
            clearStudyingSessionUseCase()
                .onSuccess {
                    _recoverySession.value = null
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

    private fun loadActivePot(uid: String, lastSelectedPotId: String){
        viewModelScope.launch {
            safeRunCatching {
                getActivePotUseCase(uid = uid, lastSelectedPotId = lastSelectedPotId)
                    .collect { pot ->
                        _displayPot.value = pot
                        updateProgressAndCondition(pot)
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

            updateProgressAndCondition(selected)

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

    //진행도 & 텍스트 계산
    private fun computeProgress(pot: Pot): Pair<Float, String> {
        val condition = PotLevelConditions.getCondition(pot.potLevel)
            ?: return 1f to "최고 레벨"

        val maxMs = condition.requiredMaxHours * 3_600_000L
        val progress = (pot.potTotalStudyingTime.toFloat() / maxMs).coerceIn(0f, 1f)

        val currentMin = pot.potTotalStudyingTime / 60_000
        val requiredMin = maxMs / 60_000
        val text = "${(progress * 100).toInt()}% (${currentMin}분/${requiredMin}분)"
        return progress to text
    }

    // 레벨업 재검증
    private suspend fun recheckLevelUpCondition() {
        val pot = _displayPot.value
        if (pot.id.isEmpty() || pot == Pot.EMPTY) {
            _uiState.update { it.copy(levelUpAvailable = false) }
            return
        }

        checkLevelUpConditionUseCase(pot.potLevel, pot.potTotalStudyingTime)
            .onSuccess { available ->
                _uiState.update { it.copy(levelUpAvailable = available) }
            }
            .onFailure {
                _uiState.update { it.copy(levelUpAvailable = false) }
            }
    }

    //레벨업 다이얼로그
    fun onLevelUpClick(potId: String, currentPotLevel: Int) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            levelUpPotUseCase(potId, currentPotLevel)
                .onSuccess {
                    _uiState.update { it.copy(isLoading = false) }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    when (e) {
                        is AppError.Custom -> {
                            if (e.message.contains("부족")) {
                                _uiState.update { it.copy(showInsufficientItemsDialog = true) }
                            } else {
                                sendToast(e.message)
                            }
                        }
                        else -> sendToast(e.message)
                    }
                }
        }
    }

    fun onDismissInsufficientDialog() {
        _uiState.update { it.copy(showInsufficientItemsDialog = false) }
    }

    //바 계산 & 레벨업 조건 검증 일괄 처리용
    private fun updateProgressAndCondition(pot: Pot) {
        val (progress, text) = computeProgress(pot)
        _uiState.update {
            it.copy(
                levelUpProgress = progress,
                levelUpProgressText = text
            )
        }
        viewModelScope.launch {
            recheckLevelUpCondition()
        }
    }
}
