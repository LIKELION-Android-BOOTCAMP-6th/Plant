package com.a32b.plant.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes {
    @Serializable data object SignIn: Routes
    @Serializable data object SignUp: Routes
    @Serializable data object HomeMain: Routes
    @Serializable data object NewBornTree: Routes
    @Serializable data object CommunityList: Routes
    @Serializable data object Mypage: Routes
    @Serializable data object ProfileEdit: Routes
    @Serializable data object MyCommunityFeed: Routes
    @Serializable data object MyPageSetting: Routes
    @Serializable data object MyPageArchive: Routes
    @Serializable data object StudyCompleted: Routes

    @Serializable data class StudyPlanDetail(val potId: String) : Routes
    @Serializable data class Studying(val potId: String, val tagId: String, val tagName: String, val title: String, val level: String): Routes
    @Serializable data class StudyResult(val timestamp: String, val tag: String,val title: String, val log: List<String>, val time: Long, val potId: String, val level: String ) : Routes

    @Serializable data class CommunityPost(val postId: String? = null, val potId: String? = null, val tagId: String? = null, val tagName: String? = null, val title: String? = null, val studyLogIds: List<String>? = emptyList()) : Routes
    @Serializable data class CommunityDetail(val postId: String) : Routes
    @Serializable data class StudyCompletedDetail(val potId: String): Routes
    @Serializable data class MyPageArchiveDetail(val potId: String): Routes


}