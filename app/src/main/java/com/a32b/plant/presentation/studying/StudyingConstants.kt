package com.a32b.plant.presentation.studying

object StudyingConstants {
    // 학습 목표 최대 개수 — 처음 진입 입력 다이얼로그 / 확인 다이얼로그 "추가하기" 공통 기준
    const val MAX_STUDY_LOG_SIZE = 10
    const val STUDY_LOG_LIMIT_MESSAGE = "학습 목표는 최대 ${MAX_STUDY_LOG_SIZE}개까지 추가할 수 있어요"
    // 최대 개수일 때 "추가하기" 버튼의 TalkBack 상태 안내 — 버튼 이름 뒤에 이어서 읽힘
    const val STUDY_LOG_LIMIT_STATE_DESCRIPTION = "최대 ${MAX_STUDY_LOG_SIZE}개 도달"
    // 최대 개수 안내 토스트 중복 방지 간격 — Toast.LENGTH_LONG 표시 시간(약 3.5초)과 맞춤
    const val STUDY_LOG_LIMIT_TOAST_INTERVAL_MS = 3_500L
    // 학습 보상을 받을 수 있는 최소 학습 시간 (1시간) — 표시용. 보상 티어 표 원본은 서버(functions/index.js STUDY_REWARD_TIERS)
    const val REWARD_MIN_STUDY_TIME_MS = 60 * 60 * 1000L
    // 결과 화면에서 보상이 이 시간 넘게 오지 않으면 "늦어지고 있어요" 안내 (함수 콜드 스타트는 수 초, 재시도는 더 걸릴 수 있음)
    const val REWARD_DELAY_NOTICE_MS = 15_000L
}
