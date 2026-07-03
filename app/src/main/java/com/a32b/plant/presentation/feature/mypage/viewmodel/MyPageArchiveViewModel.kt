package com.a32b.plant.presentation.feature.mypage.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.PotInfo
import com.a32b.plant.origin.OldPotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageArchiveUiState(
    val nickname: String = "",
    val potList: List<PotInfo> = emptyList()
)

class MyPageArchiveViewModel(private val potRepository: OldPotRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageArchiveUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    nickname = CurrentUser.nickname,
                    potList = potRepository.getUserPotsByStatus(CurrentUser.uid, true)
                )
            }
        }
    }
}


