package com.a32b.plant.ui.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.model.UserProfile
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDateTime

class HomeViewModel(private val userRepository: UserRepository) : ViewModel() {

    // 현재 로그인된 유저 ID
    //private val currentUid: String get() = CurrentUser.uid
    // 테스트용 UID
    private val currentUid: String = "ARnkLKJE60MuhYMgivXweboI6ch2"

    private val _userName = MutableStateFlow("사용자")
    val userName = _userName.asStateFlow()

    private val _currentDate = MutableStateFlow("")
    val currentDate = _currentDate.asStateFlow()

    private val _displayPot = MutableStateFlow(PotInfo())
    val displayPot = _displayPot.asStateFlow()

    private val _potList = MutableStateFlow<List<PotInfo>>(emptyList())
    val potList = _potList.asStateFlow()

    init {
        updateCurrentDate()
        observeUserProfile()
    }

    private fun updateCurrentDate() {
        val current = LocalDateTime.now()
        _currentDate.value = TimeFormatter.formatToKoreanDate(current)
    }

    private fun observeUserProfile() {
        val uid = currentUid
        if (uid.isEmpty()) return

        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest { profile ->
                profile?.let { user ->
                    _userName.value = user.nickname ?: "사용자"
                    _potList.value = user.potList

                    // 화면에 보여줄 화분 결정
                    _displayPot.value = calculateDisplayPot(user)
                }
            }
        }
    }

    private fun calculateDisplayPot(user: UserProfile): PotInfo {
        val pots = user.potList
        return when {
            // case 1: 화분이 아예 없는 경우
            pots.isEmpty() -> PotInfo(id = "", name = "화분을 추가해주세요")
            // case 2: 화분이 하나만 있는 경우
            pots.size == 1 -> pots[0]
            // case 3: 화분이 여러 개인 경우 마지막 선택한 화분 찾기
            else -> {
                val lastPot = pots.find { it.id == user.lastSelectedPotId }
                lastPot ?: pots[0]
            }
        }
    }

    fun selectPot(pot: PotInfo) {
        // 1. 즉시 UI 반영
        _displayPot.value = pot

        // 2. DB에 마지막 선택된 화분 ID 업데이트
        viewModelScope.launch {
            userRepository.updateLastSelectedPot(currentUid, pot.id)
        }
    }
}