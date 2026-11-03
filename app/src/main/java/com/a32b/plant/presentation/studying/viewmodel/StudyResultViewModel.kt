package com.a32b.plant.presentation.studying.viewmodel

import android.graphics.Bitmap
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyReward
import com.a32b.plant.domain.repository.GalleryRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.item.GiveStudyRewardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

data class StudyResultUiState(
    val isLoading: Boolean = false,
    val isDialogShow: Boolean = false,
    val isImageSaveSupported: Boolean = false,
    val reward: StudyReward? = null,
    val timestamp: String = "",
    val tag: String = "",
    val title: String = "",
    val log: List<String> = emptyList(),
    val completed: List<Boolean> = emptyList(),
    val time: Long = 0L,
    val level: String = ""
)

sealed class StudyResultEvent {
    data class ShowToast(val message: String) : StudyResultEvent()
}

@HiltViewModel
class StudyResultViewModel @Inject constructor(
    private val giveStudyRewardUseCase: GiveStudyRewardUseCase,
    private val galleryRepository: GalleryRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        savedStateHandle.toRoute<Routes.StudyResult>().let { route ->
            StudyResultUiState(
                isImageSaveSupported = galleryRepository.isGallerySaveSupported(),
                timestamp = route.timestamp,
                tag = route.tag,
                title = route.title,
                log = route.log,
                completed = route.completed,
                time = route.time,
                level = route.level
            )
        }
    )
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<StudyResultEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    init {
        // 결과 데이터는 이전 화면에서 전달받은 값이라 즉시 표시, 보상은 도착 시 표시
        // (보상 지급은 오프라인에서 완료되지 않을 수 있으므로 화면 로딩과 분리)
        loaded()
        loadReward()
    }

    fun onDialogShow() = _uiState.update { it.copy(isDialogShow = true) }
    fun onDialogDismiss() = _uiState.update { it.copy(isDialogShow = false) }

    private fun loadReward() {
        viewModelScope.launch {
            giveStudyRewardUseCase(_uiState.value.time)
                .onSuccess { reward ->
                    _uiState.update { it.copy(reward = reward) }
                }
                .onFailure { error ->
                    when (error) {
                        is AppError.UnknownUser -> Unit
                        else -> _event.send(StudyResultEvent.ShowToast(error.message))
                    }
                }
        }
    }

    fun saveBitmapToGallery(bitmap: Bitmap) {
        val fileName = "plant_${TimeFormatter.formatToDateOnly(LocalDateTime.now())}.png"

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            galleryRepository.saveImageToGallery(fileName, "image/png") { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
                .onSuccess { _event.send(StudyResultEvent.ShowToast("이미지가 저장되었습니다.")) }
                .onFailure { error -> _event.send(StudyResultEvent.ShowToast(error.message)) }
            _uiState.update { it.copy(isLoading = false) }
        }
    }
}
