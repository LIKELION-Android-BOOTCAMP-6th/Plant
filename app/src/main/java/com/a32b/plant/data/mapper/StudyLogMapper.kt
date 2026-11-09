package com.a32b.plant.data.mapper

import com.a32b.plant.core.extension.toLong
import com.a32b.plant.core.extension.toTimestamp
import com.a32b.plant.data.model.StudyLogDto
import com.a32b.plant.data.model.StudyRewardDto
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.model.StudyReward
import com.a32b.plant.domain.model.StudyRewardStatus
import com.a32b.plant.domain.type.ItemType

// 서버 트리거(functions/index.js grantStudyReward)가 쓰는 rewardStatus 값
private const val REWARD_STATUS_PENDING = "pending"
private const val REWARD_STATUS_GRANTED = "granted"

fun StudyLogDto.toDomain() : StudyLog = StudyLog(
    title = title,
    contents = contents,
    // 길이가 모자라면 미완료(false)로 채우고, 길어도 contents 길이에 맞춰 자른다 (기존 데이터 하위 호환)
    completedContents = List(contents.size) { index -> completedContents.getOrNull(index) ?: false },
    studyingTime = studyingTime,
    createAt = createAt.toLong(),
    startedAt = startedAt.toLong(),
    id = id,
    isSelected = false
)

fun StudyLog.toDto() : StudyLogDto = StudyLogDto(
    title = title,
    contents = contents,
    completedContents = completedContents,
    studyingTime = studyingTime,
    createAt = createAt.toTimestamp(),
    startedAt = startedAt.toTimestamp(),
    id = id,
)

/** 보상 지급 상태 — "granted"인데 보상 내용을 읽을 수 없으면 표시할 것이 없으므로 보상 없음으로 본다 */
fun StudyLogDto.toRewardStatus(): StudyRewardStatus = when (rewardStatus) {
    REWARD_STATUS_PENDING -> StudyRewardStatus.Pending
    REWARD_STATUS_GRANTED -> reward?.toDomain()
        ?.let { StudyRewardStatus.Granted(it, isSeen = rewardSeen) }
        ?: StudyRewardStatus.None
    // "none" 또는 이 기능 이전에 만들어진 기록("")
    else -> StudyRewardStatus.None
}

/**
 * items는 성장 아이템 키(heart, sun, water, fertilizer, nutrient)만 온다. 보너스 박스는 box 개수로 따로 온다
 * 알 수 없는 키는 건너뛴다 (서버에 새 아이템이 추가돼도 앱이 멈추지 않게)
 */
fun StudyRewardDto.toDomain(): StudyReward = StudyReward(
    items = items.mapNotNull { key -> ItemType.entries.find { it.fieldKey == key && it != ItemType.BOX } },
    boxCount = box.toInt()
)
