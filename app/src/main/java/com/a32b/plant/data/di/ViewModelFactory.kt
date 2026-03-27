package com.a32b.plant.data.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.a32b.plant.ui.feature.auth.viewmodel.SignInViewModel
import com.a32b.plant.ui.feature.auth.viewmodel.SignUpViewModel
import com.a32b.plant.ui.feature.community.viewmodel.CommunityDetailViewModel
import com.a32b.plant.ui.feature.community.viewmodel.CommunityListViewModel
import com.a32b.plant.ui.feature.community.viewmodel.CommunityPostViewModel
import com.a32b.plant.ui.feature.home.viewmodel.HomeViewModel
//import com.a32b.plant.ui.feature.home.viewmodel.HomeViewModel
import com.a32b.plant.ui.feature.home.viewmodel.NewBornTreeViewModel
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageViewModel
import com.a32b.plant.ui.feature.studying.viewmodel.StudyResultViewModel
import com.a32b.plant.ui.feature.studying.viewmodel.StudyingViewModel

object ViewModelFactory {
    val signInViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SignInViewModel(
                AppContainer.userRepository,
                AppContainer.firebaseAuth,
                AppContainer.nicknameRepository
            ) as T
        }
    }

    val signUpViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return SignUpViewModel(AppContainer.firebaseAuth, AppContainer.userRepository) as T
        }
    }
    val homeViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(AppContainer.userRepository) as T
        }
    }
    val newBornTreeViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NewBornTreeViewModel(AppContainer.potRepository) as T
        }
    }
    val myPageViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyPageViewModel(
                AppContainer.userRepository, AppContainer.potRepository,
                AppContainer.nicknameRepository
            ) as T
        }
    }
    val myPageArchiveViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HomeViewModel(AppContainer.userRepository) as T
        }
    }
    val communityListViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityListViewModel(AppContainer.postRepository) as T
        }
    }

    fun studyingViewModelFactory(
        tag: String,
        potId: String,
        title: String,
        startTime: String,
        level: String
    ) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudyingViewModel(AppContainer.studyingRepository, AppContainer.potRepository,tag, potId, title, startTime, level) as T
        }
    }

    fun studyResultViewModelFactory(
        timestamp: String,
        tag: String,
        title: String,
        log: List<String>,
        level: String
    ) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return StudyResultViewModel(
                AppContainer.potRepository,
                timestamp,
                tag,
                title,
                log,
                level
            ) as T
        }
    }

    fun communityDetailViewModelFactory(postId: String) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityDetailViewModel(AppContainer.postRepository, postId) as T
        }
    }

    val communityPostViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {

            return CommunityPostViewModel(AppContainer.postRepository) as T
        }
    }
}