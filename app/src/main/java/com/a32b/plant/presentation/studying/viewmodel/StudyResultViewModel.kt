package com.a32b.plant.presentation.studying.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyReward
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
    val reward: StudyReward? = null,
    val timestamp: String = "",
    val tag: String = "",
    val title: String = "",
    val log: List<String> = emptyList(),
    val time: Long = 0L,
    val level: String = ""
)

sealed class StudyResultEvent {
    data class ShowToast(val message: String) : StudyResultEvent()
}

@HiltViewModel
class StudyResultViewModel @Inject constructor(
    private val giveStudyRewardUseCase: GiveStudyRewardUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        savedStateHandle.toRoute<Routes.StudyResult>().let { route ->
            StudyResultUiState(
                timestamp = route.timestamp,
                tag = route.tag,
                title = route.title,
                log = route.log,
                time = route.time,
                level = route.level
            )
        }
    )
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<StudyResultEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    init {
        loadReward()
    }

    fun onDialogShow() = _uiState.update { it.copy(isDialogShow = true) }
    fun onDialogDismiss() = _uiState.update { it.copy(isDialogShow = false) }

    private fun loadReward() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            giveStudyRewardUseCase(_uiState.value.time)
                .onSuccess { reward ->
                    _uiState.update { it.copy(isLoading = false, reward = reward) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isLoading = false) }
                    when (error) {
                        is AppError.UnknownUser -> Unit
                        else -> _event.send(StudyResultEvent.ShowToast(error.message))
                    }
                }
        }
    }

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        val fileName = "plant_${TimeFormatter.formatToDateOnly(LocalDateTime.now())}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
        }
        val uri = context.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)

        uri?.let {
            context.contentResolver.openOutputStream(it)?.use { stream ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)
            }
        }
    }
}
