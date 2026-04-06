package com.a32b.plant.ui.feature.mypage.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.AppContainer.nicknameRepository
import com.a32b.plant.data.di.AppContainer.userRepository
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.repository.NicknameRepository
import com.a32b.plant.data.repository.PotRepository
import com.a32b.plant.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class MyPageSettingViewModel(
    private val userRepository: UserRepository,
    private val nicknameRepository: NicknameRepository,
    private val firebaseAuth: FirebaseAuth
) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageUiState())
    val uiState = _uiState.asStateFlow()

    private val _eventChannel = Channel<MyPageEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    fun deleteAccount() {
        viewModelScope.launch {
            try {
                // 1. 현재 로그인된 유저 정보 받기
                val uid = CurrentUser.uid
                val nickname = CurrentUser.nickname

                val firebaseUser = firebaseAuth.currentUser // 기존 auth.currentUser

                // 현재 firebase에 로그인 세션 없을 때
                // auth.currentUser가 FirebaseUser? 타입이라 nullable인데, Kotlin 컴파일러가 null 체크 없이 .delete() 호출 불허..
                if (firebaseUser == null) {
                    _eventChannel.send(MyPageEvent.ShowToast("로그인 정보가 없습니다."))
                    return@launch
                }

                // 1. Firestore 데이터 먼저 삭제 (인증 세션이 살아있는 동안)
                userRepository.deleteUser(uid)

                if (nickname.isNotBlank()) {
                    nicknameRepository.deleteNickname(nickname)
                }

                // 2. Firebase Auth 계정은 맨 마지막에 삭제
                firebaseUser.delete().await()

                // 3. 로컬 유저 정보 초기화
                CurrentUser.clear()
                _eventChannel.send(MyPageEvent.ShowToast("회원탈퇴가 완료되었습니다."))
                _eventChannel.send(MyPageEvent.NavigateToSignIn)

            } catch (e: Exception) {
                Log.e("MyPage", "회원탈퇴 실패: ${e.message}", e)

                if (e.message?.contains("RECENT_LOGIN_REQUIRED") == true) {
                    _eventChannel.send(MyPageEvent.ShowToast("보안을 위해 재로그인 후 다시 시도해주세요."))
                } else {
                    _eventChannel.send(MyPageEvent.ShowToast("회원탈퇴에 실패했습니다. 다시 시도해주세요."))
                }
            }
        }
    }
}
