package com.a32b.plant.ui.feature.mypage.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.TimeFormatter.formatToDigitalClock
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.repository.NicknameRepository
import com.a32b.plant.data.repository.PotRepository
import com.a32b.plant.data.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow


/** 데이터베이스에서 값을 받아와야 하는 경우
_변수명 : 외부에서 값을 못 건들이게 하기 위해 private으로 선언
변수명 : 외부에서 읽는 데이터.
_변수명이 바뀌면 자동으로 값이 업데이트가 되게 하기 위해 .asStaeFlow() 붙이기
 */
data class MyPageUiState(
    val nickname: String = "",
    val profileImg: String = "",
    val isUpdateSuccess: Boolean = false,
    val levelList: List<String> = emptyList(), // 프로필 편집 - 화분 이미지 띄우기 위해 쓰이는 레벨 리스트
    val isDarkMode: Boolean = false,
    val isLoading: Boolean = false,
    val nicknameError: String? = null,
    val totalStudyTime: String = "0시간 0분",
    val completedPotCount: Int = 0,
)

sealed class MyPageEvent {
    data class ShowToast(val message: String) : MyPageEvent()
    object NavigateToSignIn : MyPageEvent()// 로그인화면 보내기용 ************
    object NavigateToMyCommunityFeed: MyPageEvent()
}


class MyPageViewModel(
    private val userRepository: UserRepository,
    private val potRepository: PotRepository,
    private val nicknameRepository: NicknameRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<MyPageEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
//            potRepository.createPot()
            userRepository.getUserProfile(CurrentUser.uid).collectLatest { profile ->
                if (profile != null) {
                    _uiState.update {
                        it.copy(
                            nickname = profile.nickname ?: "이름없음",
                            profileImg = profile.profileImg ?: "",
                            isDarkMode = profile.isDarkMode ?: true,
                            totalStudyTime = formatToDigitalClock(profile.totalStudyTime ?: 0L)
                        )
                    }
                    getCompletedPotCount()
                } else {
                    Log.e("error", "-----------사용자 정보 없음")
                }
            }
        }
    }

    // 사용자의 완료 화분 개수 구해 _uiState.completedPotCount 에 넣기
    fun getCompletedPotCount() {
        viewModelScope.launch {
            try {
                val myPotList = userRepository.getUsersPots(CurrentUser.uid)
                _uiState.update { it ->
                    it.copy(
                        completedPotCount = myPotList.count { it.isCompleted }
                    )
                }
            } catch (e: Exception) {
                Log.e("error", e.message.toString())
            }
        }
    }

    // 보유한 레벨 중복 제거 레벨 리스트 가져오기
    fun getImageLevelList() {
        viewModelScope.launch {
            val resultList = potRepository.getDuplicationLevelList(CurrentUser.uid).toMutableList()
            if (resultList.isEmpty()) {
                resultList.add("")
            }
            _uiState.update { it.copy(levelList = resultList) }
        }
    }

    // 닉네임 검사용 2~10글자 허용
    private fun checkNicknameValidation(text: String): String? {
        val len = text.length
        return if (len !in 2..10) {
            "닉네임은 2자 이상 10자 이하로 입력해주세요"
        } else {
            null
        }
    }

    // 검사o, 추가o, 삭제, 업데이트?
    // 검사o, 추가o, 업데이트, 삭제?
    fun updateProfile(nickname: String, imageLevel: String) {
        val validationResult = checkNicknameValidation(nickname)
        if (validationResult != null) {
            _uiState.update {
                it.copy(
                    isUpdateSuccess = false,
                    nicknameError = validationResult
                )
            }
            return
        }
        viewModelScope.launch {
            try {
                val currentNickname = _uiState.value.nickname
//                 닉네임 같으면 프로필 사진만 변경하려는 의도로 판단
                if (nickname != currentNickname) {
////                 닉네임 중복 검사
                    if (nicknameRepository.isNicknameTaken(nickname)) {
                        _uiState.update {
                            it.copy(
                                isUpdateSuccess = false,
                                nicknameError = "이미 사용중인 닉네임입니다"
                            )
                        }
                        return@launch
                    }
                    nicknameRepository.registerNickname(nickname)
                    nicknameRepository.deleteNickname(currentNickname)
                }

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

                CurrentUser.nickname = nickname
                CurrentUser.profileImg = imageLevel

            } catch (e: Exception) {
                Log.e("error", e.message.toString())
                _uiState.update { it.copy(isUpdateSuccess = false) }
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

    fun logout() {
        // auth.signOut(): Firebase Auth 세션 제거 -> 다음 앱 실행 시 auth.currentUser == null -> 스플래시 to 로그인 화면
//        auth.signOut()
        firebaseAuth.signOut()
        // CurrentUser.clear(): 앱 메모리(CurrentUser 내 uid, nickname, profileImg 초기화
        CurrentUser.clear()
        // 로그인 화면 이동
        viewModelScope.launch {
            _eventChannel.send(MyPageEvent.NavigateToSignIn)
        }
    }

    fun moveToMyCommunityFeed() {
        viewModelScope.launch {
            _eventChannel.send(MyPageEvent.NavigateToMyCommunityFeed)
        }
    }

    //데이터베이스에서 값을 안 가져와도 되는 경우
    fun getTag() = "자격증"
}