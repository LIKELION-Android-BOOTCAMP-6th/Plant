package com.a32b.plant.presentation.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.data.local.StudyingSession
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.User
import com.a32b.plant.origin.OldPotRepository
import com.a32b.plant.origin.OldStudyingRepository
import com.a32b.plant.origin.OldUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class InterruptedUiState(
    //공부중 비정상 종료 처리를 위한 상태 관리 클래스
    val isInterrupted: Boolean = false, //비정상 종료 체크
    val interruptedStudySession: StudyingSession? = null,
    val log: List<String> = emptyList()
)
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val userRepository: OldUserRepository,
    private val studyingRepository: OldStudyingRepository,
    private val potRepository: OldPotRepository
) : BaseViewModel() {

    // 현재 로그인된 유저 ID
    private val currentUid: String get() = CurrentUser.uid
    private val _userName = MutableStateFlow("사용자")
    val userName = _userName.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate = _currentDate.asStateFlow()

    private val _displayPot = MutableStateFlow(Pot.EMPTY)
    val displayPot = _displayPot.asStateFlow()

    private val _potList = MutableStateFlow<List<Pot>>(emptyList())
    val potList = _potList.asStateFlow()

    private val _interruptedUiState = MutableStateFlow(InterruptedUiState())
    val interruptedUiState = _interruptedUiState.asStateFlow()

    init {
        updateCurrentDate()
        observeUserProfile()
        getStudySession()
    }

    fun getStudySession(){
        viewModelScope.launch {
            studyingRepository.readSession()
                .first()
                .let { session ->
                    if (session.userId == CurrentUser.uid){
                        _interruptedUiState.update { it.copy(isInterrupted = true,
                            interruptedStudySession = session) }
                    }
                }
        }
    }
    fun onInterruptedDialogDismiss() {
        _interruptedUiState.update { it.copy(isInterrupted = false) }
        viewModelScope.launch {
            studyingRepository.deleteStudyingUser()
            studyingRepository.clearSession()
        }
    }

    fun setInterruptedStudyLog(log: List<String>) = _interruptedUiState.update { it.copy(log = log) }

    fun saveStudyLog(){
        //비정상 종료 시에는 현재 날짜만 저장
        val title = TimeFormatter.formatToKoreanDate(LocalDateTime.now())
        val contents = _interruptedUiState.value.log
        val studyTime = _interruptedUiState.value.interruptedStudySession!!.time
        val studyLog = StudyLog(title,contents,studyTime!!, createAt = null, id = "", isSelected = false)
        val potId = _interruptedUiState.value.interruptedStudySession!!.potId
        potRepository.createStudyLog(potId!!, studyLog )
        potRepository.updateTotalStudyTime(potId,studyTime)
        studyingRepository.updateUserTotalStudyTime(studyTime)

    }
    private fun updateCurrentDate() {
        val current = LocalDateTime.now()
        _currentDate.value = TimeFormatter.formatWithDayOfWeek(current)
    }

    private fun observeUserProfile() {
        if (currentUid.isEmpty()) return

        viewModelScope.launch {
            userRepository.getUserProfile(currentUid).collectLatest { profile ->
                profile?.let { user ->
                    _userName.value = user.nickname ?: "사용자"

                    //레벨 DB 업데이트
                    user.potList.forEach { pot ->
                        val potId = pot.id ?: return@forEach
                        val calculatedLevel = pot.level // PotInfo 내부 로직으로 계산된 값

                        // DB에 저장된 값(imageUrl)과 계산값이 다르면 DB를 업데이트함
                        if (pot.imageUrl != calculatedLevel) {
                            potRepository.updatePotLevelOnly(potId, calculatedLevel)
                        }
                    }

                    // 완료 여부에 따른 필터 추가
                    val ongoingPots = user.potList.filter { !it.isCompleted }.toList()
                    _potList.value = ongoingPots

                    // 화면에 보여줄 화분 결정
                    _displayPot.value = calculateDisplayPot(user, ongoingPots)

                    // 빈화면 -> 홈화면
                    loaded()

                }
            }
        }
    }

    private fun calculateDisplayPot(user: User, ongoingPots: List<Pot>): Pot {
        val pots = ongoingPots
        return when {
            // case 1: 화분이 아예 없는 경우
            ongoingPots.isEmpty() -> Pot.EMPTY
            // case 2: 화분이 하나만 있는 경우
            ongoingPots.size == 1 -> ongoingPots[0]
            // case 3: 화분이 여러 개인 경우 마지막 선택한 화분 찾기
            else -> {
                val lastPot = ongoingPots.find { it.id == user.lastSelectedPotId }
                lastPot ?: ongoingPots[0]
            }
        }
    }

    fun selectPot(pot: Pot) {
        // 1. 즉시 UI 반영
        _displayPot.value = pot

        // 2. DB에 마지막 선택된 화분 ID 업데이트
        viewModelScope.launch {
            userRepository.updateLastSelectedPot(currentUid, pot.id ?: "")
        }
    }
}