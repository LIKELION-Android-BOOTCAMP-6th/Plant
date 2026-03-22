package com.a32b.plant.ui.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.AppContainer
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 시스템 스플래시 활용
 여기서 자동로그인 여부 판별해서 홈 또는 사인 인 스크린으로 이동

 */

class SplashViewModel : ViewModel(){
    private val userRepository = AppContainer.userRepository
    private val _destination = MutableSharedFlow<Routes>()
    val destination = _destination.asSharedFlow()

    init {
        checkAuthLogin()
    }

    private fun checkAuthLogin(){
        viewModelScope.launch {

            delay(3000)

            if (userRepository.isAutoLogin()) _destination.emit(Routes.HomeMain)
            else _destination.emit(Routes.SignIn)
        }
    }
}