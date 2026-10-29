package com.a32b.plant.presentation.studying

object StudyingConstants {
    // 학습 목표 최대 개수 — 처음 진입 입력 다이얼로그 / 확인 다이얼로그 "추가하기" 공통 기준
    const val MAX_STUDY_LOG_SIZE = 10
    const val STUDY_LOG_LIMIT_MESSAGE = "학습 목표는 최대 ${MAX_STUDY_LOG_SIZE}개까지 추가할 수 있어요"
    // 최대 개수일 때 "추가하기" 버튼의 TalkBack 상태 안내 — 버튼 이름 뒤에 이어서 읽힘
    const val STUDY_LOG_LIMIT_STATE_DESCRIPTION = "최대 ${MAX_STUDY_LOG_SIZE}개 도달"
    // 최대 개수 안내 토스트 중복 방지 간격 — Toast.LENGTH_LONG 표시 시간(약 3.5초)과 맞춤
    const val STUDY_LOG_LIMIT_TOAST_INTERVAL_MS = 3_500L
}
