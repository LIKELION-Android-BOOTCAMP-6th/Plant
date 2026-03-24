package com.a32b.plant.ui.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.AppContainer
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.model.UserProfile
import com.a32b.plant.core.util.TimeFormatter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
class HomeViewModel : ViewModel() {
    private val userRepository = AppContainer.userRepository

    /** 데이터베이스에서 값을 받아와야 하는 경우
    _변수명 : 외부에서 값을 못 건들이게 하기 위해 private으로 선언
    변수명 : 외부에서 읽는 데이터.
    _변수명이 바뀌면 자동으로 값이 업데이트가 되게 하기 위해 .asStaeFlow() 붙이기


     */
    // 실제 운영 시에는 Firebase Auth에서 UID를 가져와야 합니다.
    // 현재는 DB에 데이터가 없어서 예시용
    private val currentUid = "test_user_uid"
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
        _currentDate.value = TimeFormatter.formatToKoreanDate(current)    }
    private fun observeUserProfile() {
        viewModelScope.launch {
            userRepository.getUserProfile(currentUid).collectLatest { profile ->
                profile?.let { user ->
                    _userName.value = user.nickname
                    _potList.value = user.potList

                    val pots = user.potList
                    _displayPot.value = when {
                        //case 1 : 화분이 아예 없는 경우
                        pots.isEmpty() -> PotInfo(id = "", name = "화분을 추가해주세요")

                        //case 2 : 화분이 하나만 있는 경우
                        pots.size == 1 -> pots[0]

                        //case 3: 화분이 2개 이상인 경우
                        else -> {
                            val lastPot = pots.find { it.id == user.lastSelectedPotId }
                            lastPot ?: pots[0]
                        }
                    }
                }
            }
        }
    }

    fun selectPot(pot: PotInfo) {
//         1. 즉시 UI 반영 (상단 메인 카드 교체)
        _displayPot.value = pot

//         2. DB(Firestore)에 마지막 선택된 화분 ID 저장
        viewModelScope.launch {
            try {
                // userRepository에 해당 기능을 수행하는 함수가 있다고 가정합니다.
                userRepository.updateLastSelectedPot(currentUid, pot.id)
            } catch (e: Exception) {
                // 에러 처리 (필요시)
            }
        }
    }

 }