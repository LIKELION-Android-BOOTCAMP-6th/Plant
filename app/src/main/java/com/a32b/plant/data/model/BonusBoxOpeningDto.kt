package com.a32b.plant.data.model

/**
 * 보너스 박스 열기 함수(functions/index.js openBonusBoxes)의 응답 (#454)
 * items: 뽑힌 보상의 앱 ItemType 이름 목록 (예: "HEART", "GOLD_300")
 */
data class BonusBoxOpeningDto(
    var items: List<String> = emptyList()
)
