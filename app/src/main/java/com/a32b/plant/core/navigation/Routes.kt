package com.a32b.plant.core.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed interface Routes {
    @Serializable data object SignIn: Routes
    @Serializable data object HomeMain: Routes
    @Serializable data object NewBornTree: Routes
    @Serializable data object CommunityList: Routes
    @Serializable data object Mypage: Routes
    @Serializable data object AccountManage : Routes
    @Serializable data object StudyCompleted: Routes
    @Serializable data object CommunityActivity: Routes

    @Serializable data class StudyPlanDetail(val potId: String) : Routes
    @Serializable data class Studying(val potId: String, val tagId: String, val tagName: String, val title: String, val level: String): Routes
    // completed 기본값은 의도적으로 유지: 구버전에서 저장된 SavedStateHandle(프로세스 복원)에는 이 필드가 없어서,
    // 기본값을 빼면 앱 업데이트 직후 역직렬화 실패로 크래시할 수 있음
    @Serializable data class StudyResult(val timestamp: String, val tag: String,val title: String, val log: List<String>, val completed: List<Boolean> = emptyList(), val time: Long, val potId: String, val level: String ) : Routes

    @Serializable data class CommunityPost(val postId: String? = null, val potId: String? = null, val tagId: String? = null, val tagName: String? = null, val title: String? = null, val studyLogIds: List<String>? = emptyList()) : Routes
    @Serializable data class CommunityDetail(val postId: String) : Routes
    @Serializable data class StudyCompletedDetail(val potId: String): Routes
    @Serializable data object PotList : Routes
    @Serializable data class PotDetail(val potId: String) : Routes

    @Serializable data object Report : Routes

}
