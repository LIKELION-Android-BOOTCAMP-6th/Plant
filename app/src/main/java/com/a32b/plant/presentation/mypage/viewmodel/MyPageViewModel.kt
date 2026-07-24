package com.a32b.plant.presentation.mypage.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.TimeFormatter.formatToDigitalClock
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.mypage.DeleteAccountUseCase
import com.a32b.plant.domain.usecase.mypage.GetProfileImageLevelListUseCase
import com.a32b.plant.domain.usecase.mypage.LogoutUseCase
import com.a32b.plant.domain.usecase.mypage.UpdateDarkModeUseCase
import com.a32b.plant.domain.usecase.mypage.UpdateProfileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject


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
)

sealed class MyPageEvent {
    data class ShowToast(val message: String) : MyPageEvent()
    object NavigateToSignIn : MyPageEvent()// 로그인화면 보내기용 ************
    object NavigateToMyCommunityFeed : MyPageEvent()
}

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val userRepository: UserRepository,
    private val deleteAccountUseCase: DeleteAccountUseCase,
    private val getProfileImageLevelListUseCase: GetProfileImageLevelListUseCase,
    private val logoutUseCase: LogoutUseCase,
    private val updateDarkModeUseCase: UpdateDarkModeUseCase,
    private val updateProfileUseCase: UpdateProfileUseCase
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<MyPageEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            userRepository.currentUser.collectLatest { user ->
                if (user != null) {
                    _uiState.update {
                        it.copy(
                            nickname = user.nickname,
                            profileImg = user.profileImg,
                            isDarkMode = user.isDarkMode,
                            totalStudyTime = formatToDigitalClock(user.totalStudyTime)
                        )
                    }
                    // 빈화면 -> 홈화면
                    loaded()
                } else {
                    Log.e("error", "-----사용자 정보 없음")
                    loaded()
                }
            }
        }
    }

    // 보유한 레벨 중복 제거 레벨 리스트 가져오기
    fun getImageLevelList() {
        viewModelScope.launch {
            val levelList = getProfileImageLevelListUseCase()
            _uiState.update { it.copy(levelList = levelList) }
        }
    }

    fun updateProfile(nickname: String, imageLevel: String) {
        val currentNickname = uiState.value.nickname
        val currentImage = uiState.value.profileImg

        viewModelScope.launch {
            updateProfileUseCase(
                currentNickname = currentNickname,
                currentImageLevel = currentImage,
                newNickname = nickname,
                newImageLevel = imageLevel
            ).onSuccess {
                _uiState.update {
                    it.copy(
                        nickname = nickname,
                        profileImg = imageLevel,
                        isUpdateSuccess = true,
                        nicknameError = null
                    )
                }
            }.onFailure { e ->
                Log.e("MyPage", "프로필 수정 실패: ${e.message}", e)
                notifyUpdateFailure(e.message ?: "업데이트 중 오류가 발생했습니다")
            }
        }
    }

    fun resetNicknameError() {
        _uiState.update { it.copy(nicknameError = null) }
    }

    fun notifyUpdateFailure(errorMessage: String) {
        _uiState.update {
            it.copy(
                isUpdateSuccess = false,
                nicknameError = errorMessage
            )
        }
    }

    fun notifyUpdateSuccess() {
        _uiState.update {
            it.copy(
                isUpdateSuccess = true,
                nicknameError = null
            )
        }
    }

    fun clearProfileState() {
        _uiState.update {
            it.copy(
                isUpdateSuccess = false,
                nicknameError = null
            )
        }
    }

    fun resetIsUpdateSuccess() {
        _uiState.update { it.copy(isUpdateSuccess = false) }
    }


    fun toggleDarkMode() {
        val state = !uiState.value.isDarkMode
        viewModelScope.launch {
            updateDarkModeUseCase(state)
                .onSuccess {
                    _uiState.update { it.copy(isDarkMode = state) }
                }
                .onFailure { e ->
                    Log.e("MyPage", "다크모드 변경 실패: ${e.message}", e)
                }
        }
    }

    fun logout() {
        viewModelScope.launch {
            logoutUseCase()
            _eventChannel.send(MyPageEvent.NavigateToSignIn)
        }
    }

    fun moveToMyCommunityFeed() {
        viewModelScope.launch {
            _eventChannel.send(MyPageEvent.NavigateToMyCommunityFeed)
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            deleteAccountUseCase()
                .onSuccess {
                    _eventChannel.send(MyPageEvent.ShowToast("회원탈퇴가 완료되었습니다."))
                    _eventChannel.send(MyPageEvent.NavigateToSignIn)
                }
                .onFailure { e ->
                    Log.e("MyPage", "회원탈퇴 실패: ${e.message}", e)
                    val message = if (e.message?.contains("RECENT_LOGIN_REQUIRED") == true) {
                        "보안을 위해 재로그인 후 다시 시도해주세요"
                    } else {
                        e.message ?: "회원탈퇴에 실패했습니다. 다시 시도해주세요."
                    }

                    _eventChannel.send(MyPageEvent.ShowToast(message))
                }
        }
    }

    //데이터베이스에서 값을 안 가져와도 되는 경우
    fun getTag() = "자격증"
}
