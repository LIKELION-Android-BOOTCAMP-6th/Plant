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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class AttendanceUiState(
    val count: Int = 0,
    val buttonText: String = "출석하기",
    val buttonEnabled: Boolean = true
)

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
            AttendanceUiState(displayCount, "오늘 출석 완료", false)

        AttendanceDecision.MonthCompleted ->
            AttendanceUiState(displayCount, "이번 달 출석 완료", false)
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

    private val _attendanceReward = MutableStateFlow<AttendanceReward?>(null)
    val attendanceReward = _attendanceReward.asStateFlow()

    private val _attendanceErrorMessage = MutableStateFlow<String?>(null)
    val attendanceErrorMessage = _attendanceErrorMessage.asStateFlow()

    private val _isCheckingAttendance = MutableStateFlow(false)
    val isCheckingAttendance = _isCheckingAttendance.asStateFlow()

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
        if (_isCheckingAttendance.value) return

        viewModelScope.launch {
            _isCheckingAttendance.value = true

            ensureCurrentUserUseCase { user ->
                userRepository.checkAttendance(user.uid)
                    .onSuccess { reward ->
                        reward?.let { _attendanceReward.value = it }
                    }
                    .onFailure { error ->
                        when (error) {
                            is AppError.UnknownUser -> ensureCurrentUserUseCase()
                            else -> _attendanceErrorMessage.value = error.message
                        }
                    }

                // 트랜잭션 직후 getUser()의 기본 캐시 우선 조회는 옛날 값을 돌려줄 수 있어
                // refreshUser()로 캐시를 건너뛰고 서버에서 직접 다시 읽는다.
                // 성공/실패 여부와 무관하게 서버 최신 상태로 화면을 맞춘다 —
                // 실패(예: 이미 출석함)도 로컬이 알던 상태가 낡았다는 신호이기 때문.
                // 데이터 자체(출석 처리)는 위 checkAttendance()에서 이미 성공/실패가 갈렸으므로,
                // 여기 실패는 "화면 갱신만 실패"한 것 — 토스트 대신 로그로만 남긴다.
                userRepository.refreshUser(user.uid)
                    .onSuccess { refreshed ->
                        if (refreshed != null) {
                            _attendanceUiState.value = refreshed.monthCheck.toAttendanceUiState()
                        } else {
                            Log.w("HomeViewModel", "refreshUser 성공했지만 유저 문서가 null - 출석판 갱신 안 됨")
                        }
                    }
                    .onFailure { error ->
                        Log.e("HomeViewModel", "출석 후 최신 상태 재조회 실패: ${error.message}", error)
                    }
            }

            _isCheckingAttendance.value = false
        }
    }

    fun dismissAttendanceReward() {
        _attendanceReward.value = null
    }

    fun dismissAttendanceError() {
        _attendanceErrorMessage.value = null
    }
}