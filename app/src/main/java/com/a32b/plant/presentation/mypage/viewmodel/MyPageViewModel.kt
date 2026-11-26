package com.a32b.plant.presentation.mypage.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.LegalUrls
import com.a32b.plant.core.util.TimeFormatter.formatToHoursMinutes
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.usecase.auth.SignOutUseCase
import com.a32b.plant.domain.usecase.mypage.GetProfileImageLevelListUseCase
import com.a32b.plant.domain.usecase.mypage.ObserveDarkModeUseCase
import com.a32b.plant.domain.usecase.mypage.UpdateProfileUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
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
    val levelList: List<Int> = emptyList(), // 프로필 편집 - 화분 이미지 띄우기 위해 쓰이는 레벨 리스트
    val isDarkMode: Boolean = false,
    val isDarkModeUpdating: Boolean = false,
    val isLoading: Boolean = false,
    val nicknameError: String? = null,
    val totalStudyTime: String = "0분", // 누적·요약 시간의 0 표기 (#649)
    // 로그아웃 진행 중 — 확인 다이얼로그를 로딩 상태로 유지해 하단 탭·뒤로가기로 화면을 나가지 못하게 한다 (#791)
    val isLoggingOut: Boolean = false,
)

sealed class MyPageEvent {
    object NavigateToSignIn : MyPageEvent()// 로그인화면 보내기용 ************
    data class OpenUrl(val url: String) : MyPageEvent()
    data class ShowToast(val message: String, val isLong: Boolean) : MyPageEvent()
}

@HiltViewModel
class MyPageViewModel @Inject constructor(
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    private val getProfileImageLevelListUseCase: GetProfileImageLevelListUseCase,
    private val signOutUseCase: SignOutUseCase,
    private val userRepository: UserRepository,
    private val updateProfileUseCase: UpdateProfileUseCase,
    private val observeDarkModeUseCase: ObserveDarkModeUseCase
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<MyPageEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    init {
        ensureCurrentUserUseCase { user ->
            _uiState.update {
                it.copy(
                    nickname = user.nickname,
                    profileImg = user.profileImg,
                    totalStudyTime = formatToHoursMinutes(user.totalStudyTime)
                )
            }
            observeCurrentUserProfile()
            // 사용자가 확인된 경우에만 설정을 읽고, 읽은 뒤 화면을 표시한다.
            observeDarkMode()
        } ?: loaded()
    }

    // 프로필 저장이 시간 초과 뒤 서버에 늦게 반영되면 실시간 구독이 currentUser만 바꾸므로,
    // 화면의 닉네임·이미지도 따라가게 해서 옛 값으로 다시 저장하지 않게 한다.
    // 로그아웃·세션 만료 때 오는 null은 무시한다. 세션 만료 처리는 UserRepository가 따로 한다.
    private fun observeCurrentUserProfile() {
        viewModelScope.launch {
            userRepository.currentUser.filterNotNull().collect { user ->
                _uiState.update { it.copy(nickname = user.nickname, profileImg = user.profileImg) }
            }
        }
    }

    private fun observeDarkMode() {
        viewModelScope.launch {
            observeDarkModeUseCase().collect { isDarkMode ->
                _uiState.update { it.copy(isDarkMode = isDarkMode) }
                loaded()
            }
        }
    }

    // 저장된 해금 레벨까지 프로필 이미지 후보를 가져온다.
    fun getImageLevelList() {
        getProfileImageLevelListUseCase()
            .onSuccess { levelList ->
                _uiState.update { it.copy(levelList = levelList) }
            }
            .onFailure {
                _uiState.update { it.copy(levelList = listOf(0)) }
            }
    }

    fun updateProfile(nickname: String, imageLevel: String) {
        if (uiState.value.isLoading) return

        _uiState.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                updateProfileUseCase(
                    newNickname = nickname,
                    newImageLevel = imageLevel
                ).onSuccess { savedNickname ->
                    _uiState.update {
                        it.copy(
                            nickname = savedNickname,
                            profileImg = imageLevel,
                            isUpdateSuccess = true,
                            nicknameError = null
                        )
                    }
                }.onFailure { e ->
                    // 닉네임 값 자체의 문제(형식·중복)만 입력칸 인라인 에러, 네트워크·저장 실패 등 상황 에러는 토스트
                    val isInputError = e is AppError.NicknameInvalid || e is AppError.NicknameTaken
                    _uiState.update {
                        it.copy(
                            nicknameError = if (isInputError) e.message else null
                        )
                    }
                    if (!isInputError && e !is AppError.UnknownUser) {
                        _eventChannel.send(MyPageEvent.ShowToast(e.message, isLong = e is AppError.Network))
                    }
                }
            } finally {
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    fun resetNicknameError() {
        _uiState.update { it.copy(nicknameError = null) }
    }

    fun clearProfileState() {
        _uiState.update {
            it.copy(
                isUpdateSuccess = false,
                nicknameError = null
            )
        }
    }

    fun updateDarkMode(isDarkMode: Boolean) {
        if (uiState.value.isDarkModeUpdating || uiState.value.isDarkMode == isDarkMode) {
            return
        }
        _uiState.update { it.copy(isDarkModeUpdating = true) }

        viewModelScope.launch {
            userRepository.updateDarkMode(isDarkMode)
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isDarkModeUpdating = false
                        )
                    }
                }
                .onFailure {
                    _uiState.update { it.copy(isDarkModeUpdating = false) }
                }
        }
    }

    // 확인 연타로 토큰 지우기·로그아웃이 두 번 실행되지 않게 한다 (#672).
    // 마이페이지를 나가면 이 ViewModel이 정리되어 토큰 쓰기 대기 중에 취소되고 로그아웃이 실행되지 않으므로,
    // 화면은 isLoggingOut 동안 확인 다이얼로그를 로딩 상태로 유지해 나가는 경로를 막는다 (#791).
    // 실패 경로가 없고(토큰 쓰기 실패는 무시) 끝나면 로그인 화면으로 이동하므로 되돌리지 않는다
    fun logout() {
        if (uiState.value.isLoggingOut) return
        _uiState.update { it.copy(isLoggingOut = true) }
        viewModelScope.launch {
            signOutUseCase.invokeWithTokenClear()
            _eventChannel.send(MyPageEvent.NavigateToSignIn)
        }
    }

    fun onPrivacyPolicyClick() {
        viewModelScope.launch {
            _eventChannel.send(MyPageEvent.OpenUrl(LegalUrls.PRIVACY_POLICY))
        }
    }

    fun onTermsOfServiceClick() {
        viewModelScope.launch {
            _eventChannel.send(MyPageEvent.OpenUrl(LegalUrls.TERMS_OF_SERVICE))
        }
    }
}
