package com.a32b.plant.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes {
    //전달 받을 인자가 없는 일반 화면
    @Serializable data object SignIn: Routes
    @Serializable data object SignUp: Routes
    @Serializable data object HomeMain: Routes
    @Serializable data object NewBornTree: Routes
    @Serializable data object CommunityList: Routes
    @Serializable data object Mypage: Routes
    @Serializable data object ProfileEdit: Routes
    @Serializable data object MyCommunityFeed: Routes
    @Serializable data object MypageSetting: Routes
    @Serializable data object StudyCompleted: Routes


    //진입 시 데이터가 필요한 화면
    @Serializable data class StudyPlanDetail(val potId: String) : Routes
    @Serializable data class Studying(val potId: String, val tag: String, val title: String, val level: String): Routes
    //timestamp: 날짜 시작 시간~ 종료 시간
    @Serializable data class StudyResult(val timestamp: String, val tag: String,val title: String, val log: List<String>, val time: Long, val potId: String, val level: String )
    //개별 학습 기록 공유일 경우 타입에 공유 적어서 보내기
    @Serializable data class CommunityPost(val type: String? = null) : Routes
    @Serializable data class CommunityDetail(val postId: String) : Routes
    @Serializable data class StudyCompletedDetail(val potId: String): Routes
}