package com.a32b.plant.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.AutoLoginResult
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.CheckAutoLoginUseCase
import com.a32b.plant.domain.usecase.mypage.ObserveDarkModeUseCase
import com.a32b.plant.domain.usecase.pot.GetActivePotUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val isLoading: Boolean = true,
    val isDarkMode: Boolean = false,
    val isFirstScreenReady: Boolean = false
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAutoLoginUseCase: CheckAutoLoginUseCase,
    private val observeDarkModeUseCase: ObserveDarkModeUseCase,
    private val getActivePotUseCase: GetActivePotUseCase
    ) : ViewModel() {


    private val _destination = MutableStateFlow<Routes?>(null)
    val destination = _destination.asStateFlow()

    private val _uiState = MutableStateFlow(SplashUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeDarkMode()
        checkAuthLogin()
    }

    private fun observeDarkMode() {
        viewModelScope.launch {
            observeDarkModeUseCase().collect { isDarkMode ->
                _uiState.update { it.copy(isDarkMode = isDarkMode, isLoading = false) }
            }
        }
    }

    /** 첫 화면에 표시할 데이터가 준비되어 스플래시를 끝내도 될 때 호출 */
    fun onFirstScreenReady() {
        _uiState.update { it.copy(isFirstScreenReady = true) }
    }

    private fun checkAuthLogin() {
        viewModelScope.launch {
            checkAutoLoginUseCase()
                .onSuccess { result ->
                    when (result) {
                        is AutoLoginResult.LoggedIn -> startHome(result.uid, result.user.lastSelectedPotId)
                        AutoLoginResult.NotLoggedIn -> startSignIn()
                    }
                }
                .onFailure {
                    startSignIn()
                }
        }
    }

    private fun startSignIn() {
        _destination.value = Routes.SignIn
        onFirstScreenReady()
    }

    private fun startHome(uid: String, lastSelectedPotId: String) {
        _destination.value = Routes.HomeMain

        // 응답이 없어도 스플래시에 갇히지 않도록 대기 시간을 제한
        viewModelScope.launch {
            delay(MAX_HOME_WAIT_MILLIS)
            onFirstScreenReady()
        }

        // 화분이 없거나 조회에 실패하면 홈에 들어올 화분이 없으므로 기다리지 않음
        viewModelScope.launch {
            val pot = getActivePotUseCase(uid, lastSelectedPotId).firstOrNull()
            if (pot == null || pot == Pot.EMPTY) onFirstScreenReady()
        }
    }

    private companion object {
        const val MAX_HOME_WAIT_MILLIS = 3_000L
    }
}
