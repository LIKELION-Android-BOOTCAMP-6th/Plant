package com.a32b.plant.domain.repository

import com.a32b.plant.domain.model.User
import kotlinx.coroutines.flow.StateFlow

interface UserRepository {

    val currentUser : StateFlow<User?>
    fun setCurrentUser(user: User)
    fun clearCurrentUser()
    suspend fun updateLastSelectedPot(uid: String, potId: String)
}