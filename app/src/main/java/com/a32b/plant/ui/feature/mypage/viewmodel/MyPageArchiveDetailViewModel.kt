package com.a32b.plant.ui.feature.mypage.viewmodel

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter.formatToDigitalClock
import com.a32b.plant.data.di.CurrentUser
import com.a32b.plant.data.model.PotInfo
import com.a32b.plant.data.repository.PotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

//            Log.d("PlantLog", "")

data class MyPageArchiveDetailStatus(
    val nickname: String = "",
    val pot: PotInfo? = null,
    val totalStudyTime: String = "00 : 00 : 00"
)

class MyPageArchiveDetailViewModel(
    private val potRepository: PotRepository,
    val potId: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(MyPageArchiveDetailStatus())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val potInfo = potRepository.getPotById(potId) // 1. 먼저 데이터를 가져온다

            _uiState.update {
                it.copy(
                    nickname = CurrentUser.nickname,
                    pot = potInfo,
                    totalStudyTime = formatToDigitalClock(potInfo?.potTotalStudyingTime ?: 0L)
                )
            }

            Log.d("plantLog", "화분 데이터 로드 완료: ${_uiState.value.pot}")
//            Log.d("plantLog", "화분 데이터 로드 완료: ${_uiState.value.pot.logs}")
            Log.d("plantLog", "변환된 시간: ${_uiState.value.totalStudyTime}")
        }
    }
}
