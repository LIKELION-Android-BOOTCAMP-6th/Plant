package com.a32b.plant.presentation.core.extension

import com.a32b.plant.domain.type.ItemType
import kotlin.random.Random

private val ITEMS = ItemType.entries

fun Random.drawItemOnce(): ItemType {
    val target = nextDouble() * 100.0
    var cumulative = 0.0
    for (item in ITEMS){
        cumulative += item.prob
        if (target < cumulative) return item
    }
    return ItemType.HEART //오류 시 추출용
}

fun Random.drawItems(count: Int) : List<ItemType> = List(count) {drawItemOnce()}
