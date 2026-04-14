package com.a32b.plant.data.di

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserModel(
    val uid: String = "",
    val nickname: String = "",
    val profileImg: String = ""
)

// 앱 전역에서 사용하는 현재 로그인 유저 정보 싱글톤 객체
object CurrentUser {

    private val _user = MutableStateFlow<UserModel?>(null)
    val user: StateFlow<UserModel?> = _user.asStateFlow()

    val uid get() = _user.value?.uid ?: ""
    val nickname get() = _user.value?.nickname ?: ""
    val profileImg get() = _user.value?.profileImg ?: ""

    // 로그인 시 Firestore에서 받아온 유저 정보로 초기화
    fun set(userModel: UserModel) {
        _user.value = userModel
    }

    // 로그아웃 시 초기화
    fun clear() {
        _user.value = null
    }
}