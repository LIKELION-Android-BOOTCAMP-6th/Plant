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
                // 사실 없을 수가 없음
                // auth.currentUser가 FirebaseUser? 타입이라 nullable인데, Kotlin 컴파일러가 null 체크 없이 .delete() 호출 불허..
                if (firebaseUser == null) {
                    _eventChannel.send(MyPageEvent.ShowToast("로그인 정보가 없습니다."))
                    return@launch
                }

                // 2. nicknames 컬렉션에서 닉네임 문서 삭제
                nicknameRepository.deleteNickname(nickname)

                // 3. Firestore users/{uid} 문서 삭제
                userRepository.deleteUser(uid)

                // 4. Firebase Auth 계정 삭제
                firebaseUser.delete().await()

                // 5. 로컬 유저 정보 초기화
                CurrentUser.clear()

                // 6. 로그인 화면으로 이동
                _eventChannel.send(MyPageEvent.ShowToast("회원탈퇴가 완료되었습니다."))
                _eventChannel.send(MyPageEvent.NavigateToSignIn)

            } catch (e: Exception) {
                Log.e("MyPage", "회원탈퇴 실패: ${e.message}", e)

                // Firebase Auth는 최근 로그인이 아니면 재인증 필요...
                if (e.message?.contains("RECENT_LOGIN_REQUIRED") == true) {
                    _eventChannel.send(MyPageEvent.ShowToast("보안을 위해 재로그인 후 다시 시도해주세요."))
                } else {
                    _eventChannel.send(MyPageEvent.ShowToast("회원탈퇴에 실패했습니다. 다시 시도해주세요."))
                }
            }
        }
    }


}