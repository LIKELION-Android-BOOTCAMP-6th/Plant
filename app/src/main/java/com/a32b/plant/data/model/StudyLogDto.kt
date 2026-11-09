package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.PropertyName

// 읽기 전용 — 학습 기록 저장은 보안 규칙이 허용하는 필드만 보내도록 StudyingRemoteDataSourceImpl.saveStudySession이 직접 만든다 (#453)
data class StudyLogDto(
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",
    @get:PropertyName("contents") @set:PropertyName("contents")
    var contents: List<String> = emptyList(),
    @get:PropertyName("completedContents") @set:PropertyName("completedContents")
    var completedContents: List<Boolean> = emptyList(),
    @get:PropertyName("studyingTime") @set:PropertyName("studyingTime")
    var studyingTime: Long = 0L,
    @get:PropertyName("createAt") @set:PropertyName("createAt")
    var createAt: Timestamp = Timestamp.now(),
    @get:PropertyName("startedAt") @set:PropertyName("startedAt")
    var startedAt: Timestamp = Timestamp.now(),
    // 아래는 #453부터 저장되는 필드 — 이전 기록에는 없어 기본값
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    // "pending"(지급 대기) / "granted"(지급됨) / "none"(보상 없음). 이전 기록은 ""
    @get:PropertyName("rewardStatus") @set:PropertyName("rewardStatus")
    var rewardStatus: String = "",
    // 서버 트리거(grantStudyReward)만 쓴다
    @get:PropertyName("reward") @set:PropertyName("reward")
    var reward: StudyRewardDto? = null,
    // 앱이 기록에서 바꿀 수 있는 유일한 필드 — 보상을 사용자에게 보여줬으면 true
    @get:PropertyName("rewardSeen") @set:PropertyName("rewardSeen")
    var rewardSeen: Boolean = false,
    @DocumentId
    var id: String = ""
)

/** 학습 보상 — items는 users.item의 필드 키(heart, sun ...), box는 보너스 박스 개수 */
data class StudyRewardDto(
    @get:PropertyName("items") @set:PropertyName("items")
    var items: List<String> = emptyList(),
    @get:PropertyName("box") @set:PropertyName("box")
    var box: Long = 0L
)

/** 화분 ID와 함께 읽은 학습 기록 (collectionGroup 조회는 문서 경로로 화분을 알 수 있음) */
data class StudyLogWithPotDto(
    val potId: String,
    val log: StudyLogDto
)

/** 학습 기록 저장이 거부된 뒤 사유를 판단하기 위해 서버에서 읽은 상태 */
data class StudySaveStateDto(
    val logExists: Boolean,
    val potExists: Boolean,
    val potCompleted: Boolean,
    val lastStudyEndedAt: Timestamp?
)
