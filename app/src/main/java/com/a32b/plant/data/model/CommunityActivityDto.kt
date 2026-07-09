package com.a32b.plant.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.PropertyName


data class CommunityActivityDto(
    @get:PropertyName("uid") @set:PropertyName("uid")
    var uid: String = "",
    @get:PropertyName("type") @set:PropertyName("type")
    var type:String = "",
    @get:PropertyName("title") @set:PropertyName("title")
    var title:String = "",
    @get:PropertyName("targetId") @set:PropertyName("targetId")
    var targetId:String = "",
    @get:PropertyName("comment") @set:PropertyName("comment")
    var comment: String? = null,
    @get:PropertyName("commentId") @set:PropertyName("commentId")
    var commentId: String? = null,
    @get:PropertyName("createAt") @set:PropertyName("createAt")
    var createAt: Timestamp = Timestamp.now()

)
