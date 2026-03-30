package com.a32b.plant.ui.feature.mypage.viewmodel


import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.repository.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MyPageArchiveUiState(
    val nickname: String = "",
    val potList: List<PotInfo> = emptyList()
)

class MyPageArchiveViewModel(private val potRepository: PotRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(MyPageArchiveUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    nickname = CurrentUser.nickname,
                    potList = potRepository.getPotsByUserUid(CurrentUser.uid, true)
                )
            }

            Log.d("PlantLog", "받은거 viewModel 출력")
            Log.d("PlantLog", "${_uiState.value.potList}")
        }
    }
}


