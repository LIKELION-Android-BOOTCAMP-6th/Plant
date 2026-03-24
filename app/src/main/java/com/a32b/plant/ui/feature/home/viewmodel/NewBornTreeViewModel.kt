//package com.a32b.plant.ui.feature.home.viewmodel
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.viewModelScope
//import com.a32b.plant.data.di.AppContainer
//import kotlinx.coroutines.flow.MutableStateFlow
//import kotlinx.coroutines.flow.asStateFlow
//import kotlinx.coroutines.launch
//
//class NewBornTreeViewModel: ViewModel() {
//    private val userRepository = AppContainer.userRepository
//    private val currentUid = "test_user_uid" // 테스트용 id 지정
//
//    //1. DB -> 태그 내용
//    private val _dbTags = MutableStateFlow<List<String>>(emptyList())
//    val dbTags = _dbTags.asStateFlow()
//
//    //2. 상태 관리 -> 로딩, 성공 등
//    private val _isUploading = MutableStateFlow(false)
//    val isUploading = _isUploading.asStateFlow()
//
//    init {
//        fetchTags()
//    }
//
//    // 태그 획득
//    private fun fetchTags(){
//        viewModelScope.launch {
//            userRepository.getAvailableTags().collectLatest{
//                _dbTags.value = tags
//            }
//        }
//    }
//
//    //DB -> 새 화분 생성
//    fun createPot(tag: String, name: String, onSuccess: () -> Unit){
//        viewModelScope.launch {
//            _isUploading.value = true
//            val result = userRepository.addPot(currentUid, tag, name)
//            _isUploading.value = false
//
//            result.onSuccess {
//                onSuccess()
//            }
//        }
//    }
//}