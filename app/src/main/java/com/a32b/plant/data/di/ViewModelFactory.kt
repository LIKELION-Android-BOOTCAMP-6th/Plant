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
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageArchiveDetailViewModel
import com.a32b.plant.ui.feature.mypage.viewmodel.MyCommunityFeedViewModel
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageArchiveViewModel
import com.a32b.plant.ui.feature.mypage.viewmodel.MyPageSettingViewModel
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
            return HomeViewModel(AppContainer.userRepository, AppContainer.studyingRepository,
                AppContainer.potRepository) as T
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
                AppContainer.userRepository,
                AppContainer.potRepository,
                AppContainer.nicknameRepository,
                AppContainer.firebaseAuth
            ) as T
        }
    }

    val myPageSettingViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyPageSettingViewModel(
                AppContainer.userRepository,
                AppContainer.nicknameRepository,
                AppContainer.firebaseAuth
            ) as T
        }
    }
    val myPageArchiveViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyPageArchiveViewModel(AppContainer.potRepository) as T
        }
    }

    fun myPageArchiveDetailViewModelFactory(potId: String) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyPageArchiveDetailViewModel(AppContainer.potRepository, AppContainer.postRepository, potId  ) as T
        }
    }

    val communityListViewModelFactory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityListViewModel(AppContainer.postRepository) as T
        }
    }
    val myCommunityFeedViewModelFactory = object : ViewModelProvider.Factory{
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MyCommunityFeedViewModel(AppContainer.activityRepository) as T
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

    fun communityPostViewModelFactory(postId: String? = null, potId: String?=null, tag: String?=null, title: String?=null ,studyLogIds : List<String>? = emptyList()) = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CommunityPostViewModel(AppContainer.postRepository, AppContainer.potRepository, postId, potId, tag,title,studyLogIds ) as T
        }
    }

}