package com.a32b.plant.ui.feature.mypage.viewmodel

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
    init {
        viewModelScope.launch {
            _potId.value = userRepository.getPotId()
        }
    }

    //데이터베이스에서 값을 안 가져와도 되는 경우
    fun getTag() = "자격증"
}