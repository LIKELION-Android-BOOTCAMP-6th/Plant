package com.a32b.plant.ui.feature.mypage.viewmodel

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.AppContainer
import com.a32b.plant.data.model.UserProfile
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

    private val _userData = MutableStateFlow<UserProfile?>(null)
    val userData = _userData.asStateFlow()
    private val _userName = MutableStateFlow<String>("사용자")
    val userName = _userName.asStateFlow()

    // update 결과 확인용 update 완료 후 true => 다이얼로그 창 닫기
    private val _isUpdateSuccess = MutableStateFlow(false)
    val isUpdateSuccess = _isUpdateSuccess.asStateFlow()


    init {
        viewModelScope.launch {
            _potId.value = userRepository.getPotId()
            Log.d("mypage", "여기! --------------")
            Log.d("mypage", "${_potId.value} --------------")
            val result = userRepository.getUserProfile("WfFW9NVdg8NDXZPGNas4")
//            _userData.value = result
            Log.d("mypage", "여기! --------------")
            Log.d("mypage", "$result ")
        }
    }




    fun updateProfile(nickname: String, imageLevel: String) {
        if (nickname.length <= 2) {
            _isUpdateSuccess.value = false
            return
        } else {
            viewModelScope.launch {
                try {
                    userRepository.updateNicknameAndImage(
                        "WfFW9NVdg8NDXZPGNas4",
                        nickname,
                        imageLevel
                    )
                    _isUpdateSuccess.value = true
                    _userName.value = nickname
                } catch (e: Exception) {
                    Log.e("error", e.message.toString())
                    _isUpdateSuccess.value = false
                }
            }
        }
    }


    fun resetIsUpdateSuccess() {
        _isUpdateSuccess.value = false
    }


    //데이터베이스에서 값을 안 가져와도 되는 경우
    fun getTag() = "자격증"
}

//Log.d("mypage", "MyPageViewModel - ")
