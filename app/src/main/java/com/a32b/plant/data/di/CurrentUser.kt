package com.a32b.plant.data.di

// 앱 전역에서 사용하는 현재 로그인 유저 정보 싱글톤 객체
object CurrentUser {
    var uid: String = ""
    var nickname: String = ""
    var profileImg: String = ""

    // 로그인 시 Firestore에서 받아온 유저 정보로 초기화
    fun set(uid: String, nickname: String, profileImg: String) {
        this.uid = uid
        this.nickname = nickname
        this.profileImg = profileImg
    }

    // 로그아웃 시 초기화
    fun clear() {
        uid = ""
        nickname = ""
        profileImg = ""
    }
}