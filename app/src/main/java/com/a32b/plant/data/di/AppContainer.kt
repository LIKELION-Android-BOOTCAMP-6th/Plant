package com.a32b.plant.data.di

import com.a32b.plant.data.repository.ActivityRepository
import com.a32b.plant.data.repository.NicknameRepository
import com.a32b.plant.data.repository.PostRepository
import com.a32b.plant.data.repository.PotRepository
import com.a32b.plant.data.repository.StudyingRepository
import com.a32b.plant.data.repository.UserRepository

object AppContainer {
    val userRepository = UserRepository()
    val potRepository = PotRepository()
    val activityRepository = ActivityRepository()
    val postRepository = PostRepository()
    val nicknameRepository = NicknameRepository()
    val studyingRepository = StudyingRepository()
}