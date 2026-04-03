package com.a32b.plant.ui.feature.home.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.Tag
import com.a32b.plant.data.repository.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NewBornTreeViewModel(private val potRepository: PotRepository) : ViewModel() {

    private val currentUid: String get() = CurrentUser.uid

    //1. DB -> 태그 내용
    private val _dbTags = MutableStateFlow<List<Tag>>(emptyList())
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
    fun createPot(tag: Tag, name: String, onSuccess: () -> Unit){
        if(currentUid.isEmpty()){
            _errorMessage.value = "사용자 정보가 없습니다. 다시 로그인해주세요."
            return
        }
        viewModelScope.launch {
            _isUploading.value = true
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