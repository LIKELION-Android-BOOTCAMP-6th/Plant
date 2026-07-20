package com.a32b.plant.presentation.mypage.viewmodel


import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.PotRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class MyPageArchiveUiState(
    val nickname: String = "",
    val potList: List<Pot> = emptyList()
)
@HiltViewModel
class MyPageArchiveViewModel @Inject constructor(private val potRepository: PotRepository) : ViewModel() {
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


