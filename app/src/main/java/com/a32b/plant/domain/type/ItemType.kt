package com.a32b.plant.domain.type

enum class ItemType(
    val kor: String,
    val price: Int,
    val prob: Double,
    val fieldKey: String? = null
) {
    HEART("정성", 50, 60.0, "heart"),
    SUN("햇빛", 100, 13.0, "sun"),
    WATER("물", 250, 9.0, "water"),
    FERTILIZER("비료", 450, 5.0, "fertilizer"),
    NUTRIENT("영양제", 950, 2.0, "nutrient"),
    BOX("보너스 박스", 250, 5.0, "box"),
    GOLD_300("300골드", 0, 3.5),
    GOLD_500("500골드", 0, 2.0),
    GOLD_1000("1000골드", 0, 0.5)
}