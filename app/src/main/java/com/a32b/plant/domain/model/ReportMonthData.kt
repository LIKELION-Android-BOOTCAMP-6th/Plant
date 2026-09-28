package com.a32b.plant.domain.model

//리포트 계산용 확장 범위 원본 데이터. 선택 월 필터링, 기록 시각 조각 분리는 여기서 하지 않고
//Domain의 월 준비 UseCase(PrepareReportMonthUseCase)에서 한다 (Repository는 원본 조회만 담당).
data class ReportMonthData(
    val potLogs: List<ReportPotLogs>
)

data class ReportPotLogs(
    val pot: Pot,
    val logs: List<StudyLog>
)
