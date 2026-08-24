package com.a32b.plant.presentation.home.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.model.DailyCheckThisMonth
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.pot.GetActivePotUseCase
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
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
    val buttonEnabled: Boolean = true,
    val isAttendanceCompleted: Boolean = false,
    val isChecking: Boolean = false,
    val reward: AttendanceReward? = null
)

sealed class HomeEvent {
    data class ShowToast(val message: String) : HomeEvent()
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
            AttendanceUiState(displayCount, "출석하기", true)

        AttendanceDecision.AlreadyChecked ->
            AttendanceUiState(
                count = displayCount,
                buttonText = "오늘 출석 완료",
                buttonEnabled = false,
                isAttendanceCompleted = true
            )

        AttendanceDecision.MonthCompleted ->
            AttendanceUiState(
                count = displayCount,
                buttonText = "이번 달 출석 완료",
                buttonEnabled = false,
                isAttendanceCompleted = true
            )
    }
}

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActivePotUseCase: GetActivePotUseCase,
    private val getPotListUseCase: GetPotListUseCase,
    private val userRepository: UserRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
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

    //에러 상태
    private val _isLoginError = MutableStateFlow(false)
    val isLoginError = _isLoginError.asStateFlow()

    //유저 이름 상태
    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    private val _attendanceUiState = MutableStateFlow(AttendanceUiState())
    val attendanceUiState = _attendanceUiState.asStateFlow()

    private val _eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    init {
        checkUserAndLoadData()
    }
    private fun checkUserAndLoadData() {
        viewModelScope.launch {
            ensureCurrentUserUseCase { user ->
                _userName.value = user.nickname
                _attendanceUiState.value = user.monthCheck.toAttendanceUiState()

                loadActivePot(uid = user.uid, lastSelectedPotId = user.lastSelectedPotId)
                loaded()
            } ?: run {
                _isLoginError.value = true
            }
        }
    }

    private fun loadActivePot(uid: String, lastSelectedPotId: String){
        viewModelScope.launch {
            safeRunCatching {
                getActivePotUseCase(uid = uid, lastSelectedPotId = lastSelectedPotId)
                    .collect { pot ->
                        _displayPot.value = pot
                    }
            }.onFailure { throwable ->
                // 필요시 에러 핸들링
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
                                buttonEnabled = false,
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

                        refreshAttendanceState(user.uid)
                    }
            }

            _attendanceUiState.update { it.copy(isChecking = false) }
        }
    }

    private suspend fun refreshAttendanceState(uid: String) {
        userRepository.refreshUser(uid)
            .onSuccess { refreshed ->
                if (refreshed != null) {
                    _attendanceUiState.update { current ->
                        refreshed.monthCheck.toAttendanceUiState().copy(
                            isChecking = current.isChecking,
                            reward = current.reward
                        )
                    }
                } else {
                    Log.w("HomeViewModel", "refreshUser 성공했지만 유저 문서가 null - 출석판 갱신 안 됨")
                }
            }
            .onFailure { error ->
                Log.e("HomeViewModel", "출석 후 최신 상태 재조회 실패: ${error.message}", error)
            }
    }

    fun dismissAttendanceReward() {
        _attendanceUiState.update { it.copy(reward = null) }
    }
}
