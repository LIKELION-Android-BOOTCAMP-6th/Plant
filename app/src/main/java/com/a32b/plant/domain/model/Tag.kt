package com.a32b.plant.domain.model

data class Tag(
    val id: String,
    val name: String,
    val parentId: String,
    val no: Int
)