package com.a32b.plant.data.repository

import com.a32b.plant.data.di.AppContainer

class UserRepository {
    fun isAutoLogin() = true
    suspend fun getPotId() = "현재 팟 아이디"
}