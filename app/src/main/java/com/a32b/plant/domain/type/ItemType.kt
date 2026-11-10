package com.a32b.plant.domain.type

// 보너스 박스 확률 표는 서버(functions/index.js BONUS_BOX_DROP_TABLE)가 원본이다 (#454)
enum class ItemType(
    val kor: String,
    val price: Int,
    val fieldKey: String
) {
    HEART("정성", 50, "heart"),
    SUN("햇빛", 100, "sun"),
    WATER("물", 250, "water"),
    FERTILIZER("비료", 450, "fertilizer"),
    NUTRIENT("영양제", 950, "nutrient"),
    BOX("보너스 박스", 250, "box"),
    GOLD_300("300골드", 300, "gold"),
    GOLD_500("500골드", 500, "gold"),
    GOLD_1000("1000골드", 1000, "gold")
}
