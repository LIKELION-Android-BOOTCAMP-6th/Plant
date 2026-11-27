package com.a32b.plant.presentation.pot.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.SaveResult
import com.a32b.plant.domain.model.Tag
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.util.PotNameRule
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewBornTreeViewModel @Inject constructor(private val potRepository: PotRepository) : BaseViewModel() {

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
                loaded() // BaseViewModel: 최초 방출 시 LoadableScreen 해제 (#384)
            }
            // 구독 실패로 방출 없이 Flow가 끝난 경우에도 로딩 해제 → 빈 태그 목록 표시
            loaded()
        }
    }

    /**
     * DB -> 새 화분 생성.
     * [onCreated]에는 화면을 닫으면서 보여줄 안내 문구를 넘긴다 (없으면 null).
     * 오프라인(Pending)이어도 기기 캐시에 이미 반영됐으므로 화면을 닫는다 — 같은 화면에서 다시 누를 수 없게 하기 위함 (#640)
     * 단, 기다리는 사이 Activity가 다시 만들어지면 콜백이 예전 화면의 navController를 불러 화면이 닫히지 않을 수 있다
     * (미확인, 화분 삭제·완료 콜백과 같은 한계 — #629)
     */
    fun createPot(tag: Tag, name: String, onCreated: (notice: String?) -> Unit){
        // 저장 중 연타 무시 (#672). 화면 버튼의 enabled는 화면이 다시 그려져야 반영되므로 ViewModel에서도 막는다
        if (_isUploading.value) return
        if(currentUid.isEmpty()){
            _errorMessage.value = "사용자 정보가 없어요. 다시 로그인해주세요."
            return
        }
        _isUploading.value = true
        viewModelScope.launch {
            // 이름 변경과 같은 규칙으로 앞뒤 공백을 지운 뒤 저장 (#563)
            val result = potRepository.addPot(currentUid, tag, PotNameRule.normalize(name))
            _isUploading.value = false

            result.onSuccess { saveResult ->
                onCreated(if (saveResult == SaveResult.Pending) NETWORK_SLOW_MESSAGE else null)
            }
                .onFailure { exception ->
                    val msg = exception.message ?: "화분 생성에 실패했어요. 다시 시도해주세요"
                    _errorMessage.emit(msg)
                }
        }
    }
}