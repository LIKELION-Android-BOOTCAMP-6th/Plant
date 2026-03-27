package com.a32b.plant.ui.feature.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.data.di.AppContainer
import com.a32b.plant.data.di.CurrentUser
import kotlinx.coroutines.delay
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
            delay(1000)

            // autoLogin 동작 - firebase 로그인 세션 확인
            val firebaseUser = auth.currentUser

            // 로그인 세션 존재 → CurrentUser 세팅 후 홈으로
            if (firebaseUser != null) {
                val profile = userRepository.getUserProfileOnce(firebaseUser.uid)
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

//            ★★★★★★ 현재 로그아웃/회원탈퇴 기능이 없어 한번 로그인하면 자동로그인 되면서 로그인 화면으로 다시 올 수 없습니다.
//            ★★★★★★ 혹시 로그인 화면으로 돌아가고 싶으시면 29줄~44줄을 주석처리하고 49줄만 주석해제를 하십시오.
//            ★★★★★★ 로그아웃/회원탈퇴 기능 도입 시 해당 주석부분은 삭제하겠습니다.
//            _destination.value = Routes.SignIn

        }
    }
}