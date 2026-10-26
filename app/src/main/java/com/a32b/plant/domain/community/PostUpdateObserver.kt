package com.a32b.plant.domain.community

import kotlinx.coroutines.flow.Flow

interface PostUpdateObserver {
    val event: Flow<PostUpdate>
}
