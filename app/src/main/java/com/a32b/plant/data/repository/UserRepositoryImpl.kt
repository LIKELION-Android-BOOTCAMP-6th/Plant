package com.a32b.plant.data.repository

import com.a32b.plant.data.datasource.studying.StudyingRemoteDataSource
import com.a32b.plant.data.datasource.user.UserRemoteDataSource
import com.a32b.plant.domain.model.User
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.repository.UserRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val userRemoteDataSource: UserRemoteDataSource
): UserRepository {
    private val _currentUser = MutableStateFlow<User?>(null)

    override val currentUser: StateFlow<User?> = _currentUser.asStateFlow()

    override fun setCurrentUser(user: User) {
        _currentUser.value = user
    }
    override fun clearCurrentUser() {
        _currentUser.value = null
    }
}