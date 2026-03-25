package com.a32b.plant.data.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.a32b.plant.ui.feature.auth.viewmodel.SignInViewModel
import com.a32b.plant.ui.feature.auth.viewmodel.SignUpViewModel
import com.a32b.plant.ui.feature.community.viewmodel.CommunityDetailViewModel
import com.a32b.plant.ui.feature.community.viewmodel.CommunityListViewModel
import com.a32b.plant.ui.feature.community.viewmodel.CommunityPostViewModel
import com.a32b.plant.ui.feature.home.viewmodel.HomeViewModel
import com.a32b.plant.ui.feature.home.viewmodel.NewBornTreeViewModel
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageViewModel
import com.a32b.plant.ui.feature.studying.viewmodel.StudyingViewModel

object ViewModelFactory {
    val signInViewModelFactory = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SignInViewModel(AppContainer.userRepository, AppContainer.firebaseAuth) as T
        }
    }
    val signUpViewModelFactory = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SignUpViewModel(AppContainer.firebaseAuth,AppContainer.userRepository) as T
        }
    }
    val homeViewModelFactory = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(AppContainer.userRepository) as T
        }
    }
    val newBornTreeViewModelFactory = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewBornTreeViewModel(AppContainer.potRepository) as T
        }
    }
    val myPageViewModelFactory = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyPageViewModel(AppContainer.userRepository) as T
        }
    }
    val communityListViewModelFactory = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityListViewModel(AppContainer.postRepository) as T
        }
    }
    fun studyingViewModelFactory(tag: String) = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudyingViewModel(AppContainer.studyingRepository, tag) as T
        }
    }
    fun communityDetailViewModelFactory(postId : String) = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityDetailViewModel(AppContainer.postRepository, postId) as T
        }
    }
    fun communityPostViewModelFactory(type : String?) = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityPostViewModel(AppContainer.postRepository, type) as T
        }
    }
}