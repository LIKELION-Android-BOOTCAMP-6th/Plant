package com.a32b.plant.ui.feature.mypage.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.AppContainer.userRepository
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.repository.PotRepository
import com.a32b.plant.data.repository.StudyingRepository
import com.a32b.plant.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MyPageArchiveViewModel(private val userRepository: UserRepository) : ViewModel() {
    // 현재 로그인된 유저 ID
//    private val currentUid: String get() = CurrentUser.uid
    // 테스트용 UID
//    private val currentUid: String = "ARnkLKJE60MuhYMgivXweboI6ch2"

    private val _potList = MutableStateFlow<List<PotInfo>>(emptyList())
    val potList = _potList.asStateFlow()
    private val _userName = MutableStateFlow("사용자")
    val userName = _userName.asStateFlow()

    init {
        val uid =  CurrentUser.uid
        viewModelScope.launch {
            userRepository.getUserProfile(uid).collectLatest { profile ->
                profile?.let { user ->
                    _userName.value = user.nickname ?: "사용자"
                    _potList.value = user.potList
                }
            }
        }
    }
}

