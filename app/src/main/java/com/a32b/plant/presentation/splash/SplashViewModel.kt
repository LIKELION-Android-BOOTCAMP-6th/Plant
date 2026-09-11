package com.a32b.plant.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.domain.model.AutoLoginResult
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.auth.CheckAutoLoginUseCase
import com.a32b.plant.domain.usecase.mypage.ObserveDarkModeUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SplashUiState(
    val isLoading: Boolean = true,
    val isDarkMode: Boolean = false
)

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val checkAutoLoginUseCase: CheckAutoLoginUseCase,
    private val observeDarkModeUseCase: ObserveDarkModeUseCase
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
