package com.a32b.plant.ui.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.AppContainer
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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

    private val _currentPot = MutableStateFlow(PotInfo())
    val currentPot = _currentPot.asStateFlow()
    private val _currentDate = MutableStateFlow("")
    val currentDate = _currentDate.asStateFlow()

    init {
        updateCurrentDate()
        observeUserProfile()
    }
    private fun updateCurrentDate() {
        val current = LocalDate.now() // 시간 제외, 날짜만 가져옴
        val formatter = DateTimeFormatter.ofPattern("yyyy년 M월 d일")
        _currentDate.value = current.format(formatter)
    }
    private fun observeUserProfile() {
        viewModelScope.launch {
            userRepository.getUserProfile(currentUid).collectLatest { profile ->
                profile?.let {
                    _userName.value = it.nickname
                    _currentPot.value = it.currentPot
                }
            }
        }
    }
}