package com.a32b.plant.presentation.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.di.UserModel
import com.a32b.plant.domain.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SplashViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val userRepository: UserRepository
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

                // 닉네임 미설정 유저 → 로그인 화면으로 (닉네임 다이얼로그 다시 표시)
                if (profile.isFirstLogin == true) {
                    _destination.value = Routes.SignIn
                    return@launch
                }

                CurrentUser.set(
                    UserModel(
                        uid = firebaseUser.uid,
                        nickname = profile.nickname ?: "",
                        profileImg = profile.profileImg ?: ""
                    )
                )
                // 다크모드 관리용
                _isDarkMode.value = profile.isDarkMode ?: false
                observeUserTheme()

                _destination.value = Routes.HomeMain
            } else {
                // 로그인 세션 없음 → 로그인 화면
                _destination.value = Routes.SignIn
            }
        }
    }

    // 다크모드 관리용
    private fun observeUserTheme() {
        val uid = CurrentUser.uid
        viewModelScope.launch {
            userRepository.getUserFlow(uid).collect { userProfile ->
                // 데이터가 바뀌면 여기 실행
                _isDarkMode.value = userProfile?.isDarkMode ?: false
            }
        }
    }
}