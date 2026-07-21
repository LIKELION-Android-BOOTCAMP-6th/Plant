package com.a32b.plant.di

/**
 * TODO: 팀 전체 CA 전환 완료 후 제거 대상.
 *
 * 인증 계층은 이미 [com.a32b.plant.domain.repository.UserRepository.currentUser]로 전환되었으나,
 * Home/MyPage/Community/Studying 등 타 화면이 아직 전역 싱글톤 [CurrentUser]를 직접 읽고 있어
 * 해당 화면들이 깨지지 않도록 과도기 동안 양쪽에 함께 세팅해주는 브릿지.
 *
 * 팀 전체 화면이 [com.a32b.plant.domain.repository.UserRepository]로 전환되면
 * 이 파일과 [CurrentUser]를 함께 삭제하고, 이 브릿지를 호출하는 usecase들의 호출부도 제거할 것.
 */
object CurrentUserBridge {
    fun set(uid: String, nickname: String, profileImg: String) {
        CurrentUser.set(UserModel(uid = uid, nickname = nickname, profileImg = profileImg))
    }

    fun clear() {
        CurrentUser.clear()
    }
}
