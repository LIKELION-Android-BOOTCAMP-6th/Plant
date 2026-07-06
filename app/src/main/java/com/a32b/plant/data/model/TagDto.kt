package com.a32b.plant.data.model

import com.google.firebase.firestore.PropertyName

data class TagDto(
    @get:PropertyName("id") @set:PropertyName("id")
    var id: String = "",
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",
    @get:PropertyName("parentId") @set:PropertyName("parentId")
    var parentId: String = "",
    @get:PropertyName("no") @set:PropertyName("no")
    var no: Int = 0
)
