package com.a32b.plant.data.di

import com.a32b.plant.data.repository.ActivityRepository
import com.a32b.plant.data.repository.NicknameRepository
import com.a32b.plant.data.repository.PostRepository
import com.a32b.plant.data.repository.PotRepository
import com.a32b.plant.data.repository.StudyingRepository
import com.a32b.plant.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AppContainer {
    val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore by lazy { FirebaseFirestore.getInstance() }
    val userRepository = UserRepository()
    val potRepository = PotRepository()
    val activityRepository = ActivityRepository()
    val postRepository = PostRepository()
    val nicknameRepository = NicknameRepository()
    val studyingRepository = StudyingRepository()
}