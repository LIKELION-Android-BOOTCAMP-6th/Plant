package com.a32b.plant.ui.feature.mypage.viewmodel

import android.util.Log
import androidx.activity.SystemBarStyle.Companion.dark
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.TimeFormatter.formatToDigitalClock
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.repository.PotRepository
import com.a32b.plant.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 데이터베이스에서 값을 받아와야 하는 경우
_변수명 : 외부에서 값을 못 건들이게 하기 위해 private으로 선언
변수명 : 외부에서 읽는 데이터.
_변수명이 바뀌면 자동으로 값이 업데이트가 되게 하기 위해 .asStaeFlow() 붙이기
 */
data class MyPageUiState(
    val nickname: String = "",
    val profileImg: String = "Lv.1",
    val isUpdateSuccess: Boolean = false,
    val levelList: List<String> = emptyList(), // 프로필 편집 - 화분 이미지 띄우기 위해 쓰이는 레벨 리스트
    val isDarkMode: Boolean = false,
    val nicknameError: String? = null,
    val totalStudyTime: String = "0시간 0분"
)

sealed class MyPageEvent {
    object SuccessUpdate : MyPageEvent()
}


class MyPageViewModel(
    private val userRepository: UserRepository,
    private val potRepository: PotRepository
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            CurrentUser.uid = "ARnkLKJE60MuhYMgivXweboI6ch2"
//            CurrentUser.uid = "RVmMPR05kVYeLyWYknUbGdmDnGG2"
            userRepository.getUserProfile(CurrentUser.uid).collectLatest { profile ->
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            nickname = profile.nickname ?: "이름없음",
                            profileImg = profile.profileImg ?: "Lv.1",
                            isDarkMode = profile.isDarkMode ?: true,
                            totalStudyTime = formatToDigitalClock(profile.totalStudyTime ?: 0L)
                        )
                    }
                } else {
                    Log.e("PlantLog", "MyPageViewModel init - 사용자 uid 검색 결과 null")
                }
            }
        }
    }

    // 보유한 레벨 중복 제거 레벨 리스트 가져오기
    fun getImageLevelList() {
        viewModelScope.launch {
            val result = potRepository.getDuplicationLevelList(CurrentUser.uid)
            _uiState.update { it.copy(levelList = result) }
//            Log.d("PlantLog", "getImageLevelList() - $result")
        }
    }

    fun updateProfile(nickname: String, imageLevel: String) {
        if (nickname.length <= 2) {
            _uiState.update {
                it.copy(
                    isUpdateSuccess = false,
                    nicknameError = "닉네임은 3글자 이상 입력해주세요"
                )
            }
            return
        } else {
            viewModelScope.launch {
                try {
                    userRepository.updateNicknameAndImage(
                        CurrentUser.uid,
                        nickname,
                        imageLevel
                    )
                    _uiState.update {
                        it.copy(
                            nickname = nickname,
                            profileImg = imageLevel,
                            isUpdateSuccess = true,
                            nicknameError = null
                        )
                    }
                } catch (e: Exception) {
                    Log.e("error", e.message.toString())
                    _uiState.update { it.copy(isUpdateSuccess = false) }
                }
            }
        }
    }

    fun resetIsUpdateSuccess() {
        _uiState.update { it.copy(isUpdateSuccess = false) }
    }


    fun toggleDarkMode() {
        val state = !uiState.value.isDarkMode
        viewModelScope.launch {
            try {
                Log.d("plantLog", "----------3")
                userRepository.updateIsDarkMode(
                    uid = CurrentUser.uid,
                    state = state
                )
                _uiState.update { it.copy(isDarkMode = state) }

            } catch (e: Exception) {
                Log.e("error", e.message.toString())
            }
        }
    }


    //데이터베이스에서 값을 안 가져와도 되는 경우
    fun getTag() = "자격증"
}
