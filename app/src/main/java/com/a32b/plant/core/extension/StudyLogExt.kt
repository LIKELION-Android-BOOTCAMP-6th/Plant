package com.a32b.plant.core.extension

fun List<Boolean>.isCompletedAt(index: Int): Boolean = getOrNull(index) == true
