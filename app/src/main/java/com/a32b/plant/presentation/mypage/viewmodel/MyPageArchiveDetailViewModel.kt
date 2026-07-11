package com.a32b.plant.presentation.mypage.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.TimeFormatter.formatToDigitalClock
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.model.StudyLog
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.origin.OldPostRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// 체크박스 체크, 해제 - list 해당 요소 id 값 넣기, 빼기
// id list
// - id 값으로 데이터 다시 불러온다
// - or 현재 데이터 보유중, id 값으로 logList 데이터 탐색 -> 글쓰기
data class MyPageArchiveDetailStatus(
    val nickname: String = "",
    val pot: Pot? = null,
    val totalStudyTime: String = "00 : 00 : 00",
    val logs: List<StudyLog> = emptyList(),

    val selectedIds: List<String> = emptyList(),
    val isSelectionMode: Boolean = false
)

@HiltViewModel
class MyPageArchiveDetailViewModel @Inject constructor(
    private val potRepository: PotRepository,
    private val postRepository: OldPostRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {
    private val potId: String? = savedStateHandle["potId"]
    private val _uiState = MutableStateFlow(MyPageArchiveDetailStatus())
    val uiState = _uiState.asStateFlow()
    private var isUploading = false

    //선택한 학습 로그
    private val _selectedStudyLog = MutableStateFlow<StudyLog?>(null)
    val selectedStudyLog = _selectedStudyLog.asStateFlow()

    //리스트 클릭할 때
    fun onStudyLogClicked(log: StudyLog){
        _selectedStudyLog.value = log
    }

    //리스트 다이얼로그 닫을 때
    fun onDismissLogDialog(){
        _selectedStudyLog.value = null
    }


    init {
        viewModelScope.launch {
            potId?.let {
                val potInfo = potRepository.getUserPotById(CurrentUser.uid, it )
                val logList = potRepository.getPotLogs(CurrentUser.uid, it)
                _uiState.update {
                    it.copy(
                        nickname = CurrentUser.nickname,
                        pot = potInfo,
                        logs = logList,
                        totalStudyTime = formatToDigitalClock(potInfo?.potTotalStudyingTime ?: 0L)
                    )
                }
            }
        }
    }

    fun toggleSelectionMode(isEnabled: Boolean) {
        _uiState.update { currentState ->
            // 선택 모드로 변경
            if (!currentState.isSelectionMode) {
                currentState.copy(
                    isSelectionMode = true,
                    selectedIds = currentState.logs.map { it.id },
                )
            } else {
                // 일반 모드로 변경
                currentState.copy(
                    isSelectionMode = false,
                    selectedIds = emptyList()
                )
            }
        }
    }

    // 체크박스 클릭 시 실행
    fun toggleSelection(logId: String) {
        _uiState.update { currentState ->
            val currentSelected = currentState.selectedIds.toMutableList()
            // 포함되어 있으면 제거
            if (currentSelected.contains(logId)) {
                currentSelected.remove(logId)
            } else {
                currentSelected.add(logId)
            }
            currentState.copy(selectedIds = currentSelected)
        }
    }

    // 체크한 아이템 반환
    fun getSelectedLogsData(): List<StudyLog> {
        return uiState.value.logs.filter { log ->
            // id 가 포함되어 있으면 true / 없으면 제외
            uiState.value.selectedIds.contains(log.id)
        }
    }

    fun shareToPost(onSuccess: (String, String, String, List<String>) -> Unit) {
        val selectedLogs = getSelectedLogsData()
        val pot = uiState.value.pot ?: return

        // 체크박스 선택된 로그가 없으면
        if (selectedLogs.isEmpty()) return

        viewModelScope.launch {
            val potId = pot.id ?: "temp_id"
            val tag = pot.tagId ?: "기본"
            val title = pot.name ?: "제목 없음"

            val studyLogIds = selectedLogs.map { it.id }

            onSuccess(potId, tag, title, studyLogIds)
        }
    }

    fun clickAllCheckbox() {
        // db 에서 받아온 데이터 담겨있는 list
        val currentLogs = uiState.value.logs
        val selectedIds = uiState.value.selectedIds

        _uiState.update { currentState ->
            // 체크되어있는 개수와 전체 아이템 개수가 같고 아이템이 비어있지 않다면 체크 초기화
            if (selectedIds.size == currentLogs.size && currentLogs.isNotEmpty()) {
                currentState.copy(selectedIds = emptyList())
            } else {
                //
                val allIds = currentLogs.map { it.id }
                currentState.copy(selectedIds = allIds)
            }
        }
    }
}
