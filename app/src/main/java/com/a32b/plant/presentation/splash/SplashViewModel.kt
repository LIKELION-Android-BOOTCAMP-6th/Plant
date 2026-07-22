package com.a32b.plant.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.AutoLoginResult
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.CheckAutoLoginUseCase
import com.a32b.plant.origin.OldUserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAutoLoginUseCase: CheckAutoLoginUseCase,
    // 다크모드 실시간 관찰은 인증 영역이 아니므로 기존 레포지토리를 그대로 사용한다.
    private val userRepository: OldUserRepository
    ) : ViewModel() {


    private val _destination = MutableStateFlow<Routes?>(null)
    val destination = _destination.asStateFlow()

    // 다크모드 관리용
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode = _isDarkMode.asStateFlow()

    init {
        checkAuthLogin()
    }

    private fun checkAuthLogin() {
        viewModelScope.launch {
            checkAutoLoginUseCase()
                .onSuccess { result ->
                    when (result) {
                        is AutoLoginResult.LoggedIn -> {
                            _isDarkMode.value = result.user.isDarkMode
                            observeUserTheme(result.uid)
                            _destination.value = Routes.HomeMain
                        }
                        AutoLoginResult.NotLoggedIn -> {
                            _destination.value = Routes.SignIn
                        }
                    }
                }
                .onFailure {
                    _destination.value = Routes.SignIn
                }
        }
    }

    // 다크모드 관리용
    private fun observeUserTheme(uid: String) {
        viewModelScope.launch {
            userRepository.getUserFlow(uid).collect { userProfile ->
                // 데이터가 바뀌면 여기 실행
                _isDarkMode.value = userProfile?.isDarkMode ?: false
            }
        }
    }
}
