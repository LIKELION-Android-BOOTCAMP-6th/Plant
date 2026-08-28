package com.a32b.plant.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.AutoLoginResult
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.CheckAutoLoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAutoLoginUseCase: CheckAutoLoginUseCase,
    private val userRepository: UserRepository
    ) : ViewModel() {


    private val _destination = MutableStateFlow<Routes?>(null)
    val destination = _destination.asStateFlow()

    // 다크모드 관리용 — 자동로그인/수동 로그인 등 로그인 경로와 무관하게
    // currentUser 실시간 구독을 그대로 반영한다. (세션이 없으면 false)
    val isDarkMode: StateFlow<Boolean> = userRepository.currentUser
        .map { it?.isDarkMode ?: false }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), false)

    init {
        checkAuthLogin()
    }

    private fun checkAuthLogin() {
        viewModelScope.launch {
            checkAutoLoginUseCase()
                .onSuccess { result ->
                    _destination.value = when (result) {
                        is AutoLoginResult.LoggedIn -> Routes.HomeMain
                        AutoLoginResult.NotLoggedIn -> Routes.SignIn
                    }
                }
                .onFailure {
                    _destination.value = Routes.SignIn
                }
        }
    }
}
