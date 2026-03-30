package com.a32b.plant.ui.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.AppContainer
import com.a32b.plant.data.di.CurrentUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch


class SplashViewModel : ViewModel() {
    private val auth = AppContainer.firebaseAuth
    private val userRepository = AppContainer.userRepository

    private val _destination = MutableStateFlow<Routes?>(null)
    val destination = _destination.asStateFlow()

    init {
        checkAuthLogin()
    }

    private fun checkAuthLogin() {
        viewModelScope.launch {

            // autoLogin 동작 - firebase 로그인 세션 확인
            val firebaseUser = auth.currentUser

            // 로그인 세션 존재 → CurrentUser 세팅 후 홈으로
            if (firebaseUser != null) {
                val profile = userRepository.getUserProfileOnce(firebaseUser.uid)

                // 세션은 있는데 Firestore 프로필이 없으면 → 로그아웃 처리
                if (profile == null) {
                    auth.signOut()
                    _destination.value = Routes.SignIn
                    return@launch
                }

                CurrentUser.set(
                    uid = firebaseUser.uid,
                    nickname = profile?.nickname ?: "",
                    profileImg = profile?.profileImg ?: ""
                )
                _destination.value = Routes.HomeMain
            } else {
                // 로그인 세션 없음 → 로그인 화면
                _destination.value = Routes.SignIn
            }

        }
    }
}