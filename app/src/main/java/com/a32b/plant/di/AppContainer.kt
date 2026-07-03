package com.a32b.plant.di

import com.a32b.plant.PlantApplication
import com.a32b.plant.origin.OldActivityRepository
import com.a32b.plant.origin.OldNicknameRepository
import com.a32b.plant.origin.OldPostRepository
import com.a32b.plant.origin.OldPotRepository
import com.a32b.plant.origin.OldStudyingRepository
import com.a32b.plant.origin.OldUserRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

object AppContainer {
    val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firestore by lazy { FirebaseFirestore.getInstance() }
    val userRepository = OldUserRepository(firestore, firebaseAuth)
    val potRepository = OldPotRepository(firestore)
    val oldActivityRepository = OldActivityRepository(firestore)
    val postRepository = OldPostRepository(firestore)
    val nicknameRepository = OldNicknameRepository(firestore)
    val studyingRepository = OldStudyingRepository(firestore, PlantApplication.appContext)
}