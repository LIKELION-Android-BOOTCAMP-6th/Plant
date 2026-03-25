package com.a32b.plant.ui.feature.auth.viewmodel

import com.a32b.plant.data.repository.UserRepository
import com.google.firebase.auth.FirebaseAuth

class SignInViewModel(private val repository: UserRepository,private val auth: FirebaseAuth)