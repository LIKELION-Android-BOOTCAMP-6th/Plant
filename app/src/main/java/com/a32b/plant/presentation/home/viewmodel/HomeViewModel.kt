package com.a32b.plant.presentation.home.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.KST_ZONE_ID
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyingSession
import com.a32b.plant.domain.model.AttendanceDecision
import com.a32b.plant.domain.model.AttendanceReward
import com.a32b.plant.domain.model.DailyCheckThisMonth
import com.a32b.plant.domain.model.Item
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.PotLevelConditions
import com.a32b.plant.domain.model.StudyReward
import com.a32b.plant.domain.model.StudySaveResult
import com.a32b.plant.domain.model.UnseenStudyReward
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
import com.a32b.plant.domain.usecase.studying.GetRecoverySessionUseCase
import com.a32b.plant.domain.usecase.studying.RecoverStudySessionUseCase
import com.a32b.plant.presentation.studying.service.StudyingTimerServiceController
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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
    val levelUpReady: Boolean = false, // 레벨업 학습 시간 조건 충족 (false면 버튼을 비활성처럼 표시, 누르면 LevelUpPotUseCase가 이유 안내 — 남은 시간·최고 레벨 등)
    val insufficientItemsMessage: String? = null, // 아이템 부족 다이얼로그 문구 ("햇빛, 물이(가) 부족합니다.") — null이면 닫힘
    val levelUpProgress: Float = 0f,
    val levelUpProgressText: String = "",
    val isLoading: Boolean = false
)

sealed class HomeEvent {
    data class ShowToast(val message: String) : HomeEvent()
    data class BonusBoxResult(val items: List<ItemType>) : HomeEvent()
    // isPending: 서버 응답 없이 기기에만 반영됨 (오프라인) — 연결되면 자동 전송, 다시 보내지 않음
    data class LevelUpSuccess(val isPending: Boolean) : HomeEvent()
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

// 유저 정보로 출석 상태를 다시 계산하되, 진행 중인 보상 다이얼로그와 출석 처리 중 상태는 유지한다
private fun DailyCheckThisMonth.toAttendanceUiStateKeeping(
    current: AttendanceUiState
): AttendanceUiState = toAttendanceUiState().copy(
    reward = current.reward,
    isChecking = current.isChecking
)

/**
 * 홈 "오늘 학습 시간"의 0 표시 — 화분 있음(기록 0)·화분 없음·조회 실패를 같은 형식으로 맞춘다 (#584)
 * formatToDigitalClock은 학습 타이머·알림·결과 화면 등 여러 곳에서 쓰므로 반환 형식을 바꾸지 않고, 0을 이 함수 결과에 맞춘다
 */
internal val ZERO_STUDY_TIME: String = TimeFormatter.formatToDigitalClock(0L)

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
    private val openBonusBoxUseCase: OpenBonusBoxUseCase,
    private val checkLevelUpConditionUseCase: CheckLevelUpConditionUseCase,
    private val levelUpPotUseCase: LevelUpPotUseCase,
    private val studyingTimerServiceController: StudyingTimerServiceController
) : BaseViewModel() {

    private val _displayPot = MutableStateFlow(Pot.EMPTY)
    val displayPot = _displayPot.asStateFlow()

    // 마지막으로 선택한 대표 화분 ID — 처음엔 저장된 lastSelectedPotId, 화분 변경 시 갱신 (#582)
    // 화분 목록이 다시 방출될 때 이 값으로 대표 화분을 고르므로, 저장이 늦거나 실패해도 화면은 선택한 화분을 유지
    private val selectedPotId = MutableStateFlow("")
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

    private val _showAttendanceDialog = MutableStateFlow(false)
    val showAttendanceDialog = _showAttendanceDialog.asStateFlow()
    private var lastAutoAttendanceDate: LocalDate? = null

    // 일회성 화면 이벤트
    private val _eventChannel = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    // 오늘 학습 시간 상태 — null은 조회 중 (화면은 자리만 차지하고 글자를 숨김, #584)
    // 0은 화분 있음·없음·조회 실패 모두 formatToDigitalClock(0) 형식으로 맞춘다
    private val _todayStudyTime = MutableStateFlow<String?>(null)
    val todayStudyTime = _todayStudyTime.asStateFlow()

    // 진행 중인 오늘 학습 시간 조회 — 새 조회를 시작하면 취소해, 늦게 끝난 이전 화분의 결과가 덮어쓰지 않게 함 (#584)
    private var todayStudyTimeJob: Job? = null


    //아이템 상태
    private val _userItem = MutableStateFlow(Item(0, 0, 0, 0, 0, 0))
    val userItem = _userItem.asStateFlow()

    // 복구 세션 상태
    private val _recoverySession = MutableStateFlow<StudyingSession?>(null)
    val recoverySession = _recoverySession.asStateFlow()

    // 복구 처리 중 로딩
    private val _isRecovering = MutableStateFlow(false)
    val isRecovering = _isRecovering.asStateFlow()

    // 진행 중인 홈 복귀 처리(복구 세션 확인 → 자동 출석 판단)
    // 새 복귀나 저장·폐기를 시작하면 취소해 늦게 끝난 확인이 지운 세션을 되살리지 않게 함
    private var recoveryCheckJob: Job? = null

    //박스 개봉 로딩
    private val _isBoxOpening = MutableStateFlow(false)
    val isBoxOpening = _isBoxOpening.asStateFlow()

    // 레벨업 상태
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState = _uiState.asStateFlow()

    // 지급됐지만 아직 보여주지 않은 학습 보상 (오프라인에서 저장한 세션, 결과 화면을 못 본 세션) — #453
    private val _unseenStudyRewards = MutableStateFlow<List<UnseenStudyReward>>(emptyList())
    // 쌓인 보상 창에 표시할 합계 — 없으면 null (창을 띄우지 않음)
    val unseenStudyReward: StateFlow<StudyReward?> = _unseenStudyRewards
        .map { rewards -> rewards.takeIf { it.isNotEmpty() }?.let { StudyReward.sum(it.map { unseen -> unseen.reward }) } }
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    init {
        checkUserAndLoadData()
        observeUserItem()
        observeUnseenStudyRewards()
    }

    /**
     * 서버 트리거가 지급한 학습 보상 중 아직 확인하지 않은 것을 구독 — 쌓인 보상은 HomeScreen이 합쳐서 한 번에 보여준다
     * 결과 화면이 보여준 보상은 그 화면에서 확인 표시하므로 여기에는 남지 않는다
     * 구독 실패(로그아웃 후 권한 거부, 색인 미배포 등)는 보상 지급과 무관하므로 로그만 남김
     * 유저가 없을 때의 세션 만료 알림은 init의 checkUserAndLoadData가 보내므로, 두 번 가지 않도록 현재 값을 직접 읽음
     */
    private fun observeUnseenStudyRewards() {
        val uid = userRepository.currentUser.value?.uid ?: return
        viewModelScope.launch {
            studyingRepository.observeUnseenStudyRewards(uid)
                .catch { e -> Log.e("HomeViewModel", "쌓인 학습 보상 구독 실패", e) }
                .collect { rewards -> _unseenStudyRewards.value = rewards }
        }
    }

    /** 쌓인 보상 창 확인 — 표시한 기록들의 rewardSeen = true (기기 캐시에 바로 반영되어 창이 닫힘) */
    fun confirmUnseenStudyRewards() {
        val shown = _unseenStudyRewards.value
        if (shown.isEmpty()) return
        // 쓰기 결과를 기다리는 동안 창이 다시 그려지지 않도록 먼저 비운다 (구독이 곧 같은 결과를 보냄)
        _unseenStudyRewards.value = emptyList()
        viewModelScope.launch {
            ensureCurrentUserUseCase { user ->
                studyingRepository.markStudyRewardsSeen(user.uid, shown.map { it.ref })
            }
        }
    }
    private fun checkUserAndLoadData() {
        viewModelScope.launch {
            ensureCurrentUserUseCase { user ->
                _userName.value = user.nickname
                _attendanceUiState.value = user.monthCheck.toAttendanceUiState()
                _userItem.value = user.item

                selectedPotId.value = user.lastSelectedPotId
                // 로딩 해제는 첫 화분을 받은 뒤 loadActivePot 안에서 한다 (#487)
                loadActivePot(uid = user.uid)
            } ?: loaded() // 현재 유저 없음: 세션 만료 처리는 ensureCurrentUserUseCase가 하고, 로딩만 해제
        }
    }

    private fun maybeShowAutomaticAttendanceDialog() {
        if (_recoverySession.value != null) return

        val today = LocalDate.now(KST_ZONE_ID)
        if (lastAutoAttendanceDate == today) return

        lastAutoAttendanceDate = today
        if (!_attendanceUiState.value.isAttendanceCompleted) {
            _showAttendanceDialog.value = true
        }
    }

    // 상점 화면 구현 전까지는 "준비 중" 토스트만 표시 (구현 후 네비게이션 이벤트로 교체)
    fun onShopClick() {
        sendToast("준비 중인 기능입니다")
    }

    fun openAttendanceDialog() {
        _showAttendanceDialog.value = true
    }

    fun closeAttendanceDialog() {
        _showAttendanceDialog.value = false
    }

    /**
     * 홈이 보일 때마다(ON_RESUME) 호출 — 첫 진입 포함
     * 1. 저장되지 않은 학습 세션 확인 (결과 화면에서 돌아올 때는 ViewModel이 유지되어 init이 다시 실행되지 않음)
     * 2. 복구 확인이 끝난 뒤에 자동 출석 판단 (두 창이 겹치지 않도록 순서 고정)
     * 복구 처리 중에는 지워지기 전의 백업을 다시 읽어 창이 되살아나지 않도록 건너뜀
     */
    fun onHomeResumed() {
        // 학습 화면은 결과 화면 이동, 에러 확인 후 홈 이동, 저장 안 된 종료 기록 발견 시 홈 이동으로 떠나며
        // 이 경로들은 타이머를 멈추거나 시작하지 않으므로 홈이 보이는 동안 돌아야 할 타이머는 없다.
        // 세션 만료로 로그인 화면에 가는 경우는 MainActivity의 세션 만료 처리에서 타이머를 멈춘다 (#545)
        // 학습 화면에 새 이탈 경로를 만들면 이 전제를 함께 확인할 것
        // 학습 화면 없이 홈으로 들어온 경우(화면 상태 저장 전 프로세스 종료, 밀어서 종료 등) 남은 서비스와 알림을 정리 (#514, #509)
        // 복구 창 처리보다 먼저 멈춰, 창을 처리하지 않고 나가도 알림이 남지 않게 함
        studyingTimerServiceController.stop()
        if (_isRecovering.value) return
        recoveryCheckJob?.cancel()
        recoveryCheckJob = viewModelScope.launch {
            getRecoverySessionUseCase()
                .onSuccess { session -> _recoverySession.value = session }
            // 조회가 실패해도 판단을 진행한다. 실패 종류는 UnknownUser(아래 decideAutomaticAttendance가 currentUser null로 조용히 반환)
            // 와 로컬 읽기 실패(이번 복귀에는 복구 창을 못 띄우며, 이후 백업이 발견되면 HomeScreen의 recoverySession == null 조건이 출석 창을 숨김)
            // 초기 로딩이 끝난 뒤(첫 화분 반영 후) 출석을 판단해, 로딩 화면 위에 출석 창이 먼저 뜨지 않게 한다 (#487)
            isLoading.first { !it }
            decideAutomaticAttendance()
        }
    }

    private fun decideAutomaticAttendance() {
        // 같은 날 복귀는 아직 갱신되지 않은 유저 정보가 방금 한 출석 상태를 덮어쓰지 않도록 건너뛴다
        if (lastAutoAttendanceDate == LocalDate.now(KST_ZONE_ID)) return

        // 복구 세션 조회(getRecoverySessionUseCase)가 이미 ensureCurrentUserUseCase로 세션 만료를 알렸으므로
        // 이벤트 중복을 막기 위해 여기서는 현재 값을 직접 읽고, 값이 없으면 이벤트 없이 반환한다
        val user = userRepository.currentUser.value ?: return
        _attendanceUiState.update { user.monthCheck.toAttendanceUiStateKeeping(it) }
        maybeShowAutomaticAttendanceDialog()
    }

    private fun observeUserItem() {
        viewModelScope.launch {
            userRepository.currentUser.collect { currentUser ->
                currentUser?.let {
                    _userItem.value = it.item
                    _userName.value = it.nickname

                    _attendanceUiState.update { currentUiState ->
                        it.monthCheck.toAttendanceUiStateKeeping(currentUiState)
                    }
                }
            }
        }
    }

    /**
     * 복구 저장 (#453)
     * - 저장됨 / 이미 저장됨: 창 닫기 (같은 세션 ID라 중복 저장 없음)
     * - 대기 중(오프라인): 창 닫기, 백업은 유지 — 연결되면 자동 전송되고 다음 홈 복귀에서 서버 반영을 확인해 정리
     * - 다시 보내도 안 되는 거부: 사유 안내 후 창 닫기 (백업은 유즈케이스가 지움)
     * - 저장 실패(서버 상태 확인 불가 등): 창을 유지해 다시 선택할 수 있게 함
     */
    fun recoverSession() {
        val session = _recoverySession.value ?: return
        if (_isRecovering.value) return
        recoveryCheckJob?.cancel()
        _isRecovering.value = true
        viewModelScope.launch {
            recoverStudySessionUseCase(session)
                .onSuccess { result ->
                    _recoverySession.value = null
                    maybeShowAutomaticAttendanceDialog()
                    sendToast(
                        when (result) {
                            StudySaveResult.Saved, StudySaveResult.AlreadySaved -> "학습 기록이 저장되었습니다."
                            StudySaveResult.Pending -> "네트워크에 연결되면 학습 기록이 자동으로 저장돼요."
                            is StudySaveResult.Rejected -> result.reason.message
                        }
                    )
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
                    maybeShowAutomaticAttendanceDialog()
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

    /**
     * 대표 화분을 실시간으로 구독하고, 첫 화분(화분이 없으면 Pot.EMPTY)을 반영한 뒤 초기 로딩을 해제한다 (#487)
     * 실시간 구독이라 collect가 끝나지 않으므로 해제는 collect 안에서 한다.
     * 조회 실패는 PotRepositoryImpl.getPots의 catch가 처리해 아무 값도 방출되지 않고 Flow가 끝날 수 있어,
     * collect가 끝난 뒤에도 해제해 로딩에 갇히지 않게 한다.
     */
    private fun loadActivePot(uid: String){
        viewModelScope.launch {
            safeRunCatching {
                getActivePotUseCase(uid = uid, selectedPotId = selectedPotId)
                    .collect { pot ->
                        val previousPotId = _displayPot.value.id
                        _displayPot.value = pot
                        updateProgressAndCondition(pot)
                        if (pot.id.isNotEmpty() && pot != Pot.EMPTY) {
                            // 화분 목록이 다시 방출됐는데(어느 화분 문서든 바뀌면) 대표 화분이 그대로면 조회하는 동안 지금 값을 그대로 둔다
                            // 다른 화분으로 바뀌었으면 이전 화분의 시간이 보이지 않게 조회 중으로 돌린다 (#584)
                            fetchTodayStudyTime(uid, pot.id, keepCurrent = pot.id == previousPotId)
                        } else {
                            todayStudyTimeJob?.cancel()
                            _todayStudyTime.value = ZERO_STUDY_TIME
                        }
                        // 화분을 반영한 뒤 해제해야 첫 화면에 기본 화면(Pot.EMPTY)이 그려지지 않는다
                        // 두 번째 방출부터는 이미 해제된 상태라 변화 없음
                        loaded()
                    }
            }.onFailure { throwable ->
                // 필요시 에러 핸들링
            }
            // 방출 없이 끝났거나(조회 실패, uid 없음) 예외로 끝난 경우의 해제
            loaded()
        }
    }

    private fun sendToast(message: String) {
        viewModelScope.launch {
            _eventChannel.send(HomeEvent.ShowToast(message))
        }
    }

    /**
     * 화분 하나의 오늘 학습 시간 조회
     * keepCurrent = false면 조회 중(null)으로 돌려 이전 값이 보이지 않게 한다 (첫 진입·화분 변경)
     * 실패하면 다른 상태와 같은 형식의 0을 보여주고 토스트로 안내한다
     */
    private fun fetchTodayStudyTime(uid: String, potId: String, keepCurrent: Boolean = false) {
        todayStudyTimeJob?.cancel()
        if (!keepCurrent) _todayStudyTime.value = null
        todayStudyTimeJob = viewModelScope.launch {
            getTodayTotalStudyTimeUseCase(uid, potId)
                .onSuccess { totalMillis ->
                    _todayStudyTime.value = TimeFormatter.formatToDigitalClock(totalMillis)
                }
                .onFailure { error ->
                    _todayStudyTime.value = ZERO_STUDY_TIME
                    sendToast(error.message)
                }
        }
    }

    fun setTempSelectedPot(pot: Pot) {
        _tempSelectedPot.value = pot
    }

    fun confirmPotChange() {
        _tempSelectedPot.value?.let { selected ->
            // 다이얼로그는 지금 화분이 선택된 채 열리므로, 바로 선택 완료를 누르면 같은 화분이다
            // _displayPot을 바꾸기 전에 비교해야 한다 (바꾼 뒤에는 항상 같음)
            val isSamePot = selected.id == _displayPot.value.id
            // 화분 목록이 다시 방출될 때도 이 화분을 고르도록 먼저 바꾼다 (#582)
            selectedPotId.value = selected.id
            _displayPot.value = selected
            // 다른 화분이면 새 화분의 조회가 끝나기 전에 이전 화분의 시간이 보이지 않도록 바로 조회 중으로 돌린다 (#584)
            // 같은 화분이면 지금 값이 그 화분의 값이므로 그대로 두고 다시 조회만 한다 (숨겼다 보이는 깜빡임 방지)
            if (!isSamePot) {
                todayStudyTimeJob?.cancel()
                _todayStudyTime.value = null
            }

            updateProgressAndCondition(selected)

            viewModelScope.launch {
                safeRunCatching {
                    ensureCurrentUserUseCase { user ->
                        // 저장은 서버 응답까지 기다리므로(오프라인이면 연결될 때까지) 오늘 학습 시간을 먼저 조회한다
                        // 같은 화분이어도 저장은 다시 한다 (이전 저장이 거부됐다면 다시 저장하는 경로가 됨)
                        fetchTodayStudyTime(user.uid, selected.id, keepCurrent = isSamePot)
                        userRepository.updateLastSelectedPot(user.uid, selected.id)
                    }
                }.onFailure { e ->
                    // 화면은 selectedPotId로 선택한 화분을 유지한다. 저장이 거부되면 홈을 새로 만들 때(앱 재시작 등) 이전 화분으로 보일 수 있음
                    Log.e("HomeViewModel", "대표 화분 저장 실패", e)
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

        val maxMs = condition.maxHoursForProgress * 3_600_000L
        val progress = (pot.potTotalStudyingTime.toFloat() / maxMs).coerceIn(0f, 1f)

        val currentMin = pot.potTotalStudyingTime / 60_000
        val requiredMin = maxMs / 60_000
        val text = "${(progress * 100).toInt()}% (${currentMin}분/${requiredMin}분)"
        return progress to text
    }

    // 레벨업 재검증
    private fun recheckLevelUpCondition(pot: Pot) {
        val ready = pot.id.isNotEmpty() &&
                pot != Pot.EMPTY &&
                checkLevelUpConditionUseCase(pot.potLevel, pot.potTotalStudyingTime)

        _uiState.update { it.copy(levelUpReady = ready) }
    }

    // 레벨업 (#456): 조건 확인·안내는 LevelUpPotUseCase가 하고, 여기서는 결과만 화면에 알린다
    // 레벨 이미지·아이템 개수는 화분·유저 실시간 구독이 기기 캐시 반영 즉시 갱신하므로 직접 바꾸지 않는다
    // (서버에서 거부되면 캐시가 되돌아가 구독이 원래 값으로 다시 그린다)
    fun onLevelUpClick(potId: String) {
        // 처리 중 연타 가드 — 같은 레벨업 batch가 두 번 가지 않게 한다 (두 번 가도 규칙이 두 번째를 거부함)
        if (_uiState.value.isLoading) return
        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            levelUpPotUseCase(potId)
                .onSuccess { result ->
                    _uiState.update { it.copy(isLoading = false) }
                    _eventChannel.send(HomeEvent.LevelUpSuccess(isPending = result == SaveResult.Pending))
                }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false) }
                    when (e) {
                        is AppError.UnknownUser -> Unit
                        is AppError.InsufficientItems -> {
                            _uiState.update { it.copy(insufficientItemsMessage = e.message) }
                        }
                        else -> sendToast(e.message)
                    }
                }
        }
    }

    fun onDismissInsufficientDialog() {
        _uiState.update { it.copy(insufficientItemsMessage = null) }
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
        recheckLevelUpCondition(pot)
    }
}
