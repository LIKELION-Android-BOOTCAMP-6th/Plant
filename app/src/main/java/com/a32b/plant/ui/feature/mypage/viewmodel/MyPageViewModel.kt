package com.a32b.plant.ui.feature.mypage.viewmodel

import android.util.Log
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.AppContainer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MyPageViewModel : ViewModel() {
    private val userRepository = AppContainer.userRepository

    /** 데이터베이스에서 값을 받아와야 하는 경우
    _변수명 : 외부에서 값을 못 건들이게 하기 위해 private으로 선언
    변수명 : 외부에서 읽는 데이터.
    _변수명이 바뀌면 자동으로 값이 업데이트가 되게 하기 위해 .asStaeFlow() 붙이기
     */
    private val _potId = MutableStateFlow<String>("")
    val potId = _potId.asStateFlow()
    private val _userName = MutableStateFlow("사용자")
    val userName = _userName.asStateFlow()

    init {
        viewModelScope.launch {
            _potId.value = userRepository.getPotId()
        }
    }

    // repository 는 suspend 사용
    // 여기에서는 launch 와 async 중 launch 사용
    fun updateProfile(nickname: String, imageLevel: String) {
        Log.d("mypage", "MyPageViewModel - $nickname ") // Log.d nickname 출력 확인 완료
        viewModelScope.launch {
            _userName.value = nickname
//            userRepository.updateNicknameAndImage("WfFW9NVdg8NDXZPGNas4", nickname, imageLevel) repository
        }
    }


    //데이터베이스에서 값을 안 가져와도 되는 경우
    fun getTag() = "자격증"
}

//Log.d("mypage", "MyPageViewModel - ")
