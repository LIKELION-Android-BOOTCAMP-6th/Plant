package com.a32b.plant.ui.feature.home.viewmodel

import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.repository.PotRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NewBornTreeViewModel(
    private val potRepository: PotRepository
): ViewModel() {

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val currentUid: String get() = auth.currentUser?.uid?: ""
    private val currentUidTest = "test_user_uid" // 테스트용 id 지정

    //1. DB -> 태그 내용
    private val _dbTags = MutableStateFlow<List<String>>(emptyList())
    val dbTags = _dbTags.asStateFlow()

    //2. 상태 관리 -> 로딩, 성공 등
    private val _isUploading = MutableStateFlow(false)
    val isUploading = _isUploading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String>("")
    val errorMessage = _errorMessage.asStateFlow()

    init {
        fetchTags()
    }

    // 태그 획득
    private fun fetchTags(){
        viewModelScope.launch {
            potRepository.getAvailableTags().collectLatest { tags ->
                _dbTags.value = tags
            }
        }
    }

    //DB -> 새 화분 생성
    fun createPot(tag: String, name: String, onSuccess: () -> Unit){
        viewModelScope.launch {
            val result = potRepository.addPot(currentUid, tag, name)
            _isUploading.value = false

            result.onSuccess { onSuccess() }
                .onFailure { exception ->
                    val msg = exception.message ?: "화분 생성에 실패했습니다. 다시 시도해주세요"
                    _errorMessage.emit(msg)
                }
        }
    }
}