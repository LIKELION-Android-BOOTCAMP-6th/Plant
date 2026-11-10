package com.a32b.plant.domain.model

import com.a32b.plant.domain.type.ItemType

data class StudyReward(
    val items: List<ItemType>,
    val boxCount: Int
) {
    /** 화면 표시용 — 보너스 박스도 아이템 하나로 펼친 목록 (같은 아이템은 개수만큼 반복) */
    fun toItemList(): List<ItemType> = items + List(boxCount) { ItemType.BOX }

    companion object {
        /** 여러 세션의 보상을 합친다 (홈의 쌓인 보상 창) */
        fun sum(rewards: List<StudyReward>): StudyReward = StudyReward(
            items = rewards.flatMap { it.items },
            boxCount = rewards.sumOf { it.boxCount }
        )
    }
}

/**
 * 학습 기록 하나의 보상 지급 상태 (#453)
 * 보상은 앱이 아니라 서버 트리거(grantStudyReward)가 기록이 서버에 생긴 뒤 지급한다
 */
sealed interface StudyRewardStatus {
    /** 지급 대기 중 — 오프라인이라 아직 서버에 없거나, 트리거가 처리 중 */
    data object Pending : StudyRewardStatus

    /** 보상 없음 (1시간 미만, 또는 이 기능 이전에 만들어진 기록) */
    data object None : StudyRewardStatus

    /** 지급됨 */
    data class Granted(val reward: StudyReward, val isSeen: Boolean) : StudyRewardStatus
}

/** 학습 기록 문서 위치 (users/{uid}/pots/{potId}/logs/{logId}) */
data class StudyLogRef(
    val potId: String,
    val logId: String
)

/** 지급됐지만 아직 사용자에게 보여주지 않은 학습 보상 (결과 화면을 못 봤거나 오프라인에서 저장한 세션) */
data class UnseenStudyReward(
    val ref: StudyLogRef,
    val reward: StudyReward
)
