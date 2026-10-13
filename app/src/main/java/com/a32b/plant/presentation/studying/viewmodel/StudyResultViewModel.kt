package com.a32b.plant.presentation.studying.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.TimeFormatter
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
    val isDialogShow: Boolean = false,
    val reward: StudyReward? = null,
    val isRewardLoaded: Boolean = false
)

sealed class StudyResultEvent {
    data class ShowToast(val message: String) : StudyResultEvent()
}

@HiltViewModel
class StudyResultViewModel @Inject constructor(
    private val giveStudyRewardUseCase: GiveStudyRewardUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyResultUiState())
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<StudyResultEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    fun onDialogShow() = _uiState.update { it.copy(isDialogShow = true) }
    fun onDialogDismiss() = _uiState.update { it.copy(isDialogShow = false) }

    fun loadReward(sessionTimeMs: Long) {
        if (_uiState.value.isRewardLoaded) return
        viewModelScope.launch {
            giveStudyRewardUseCase(sessionTimeMs)
                .onSuccess { reward ->
                    _uiState.update { it.copy(reward = reward, isRewardLoaded = true) }
                }
                .onFailure { error ->
                    _uiState.update { it.copy(isRewardLoaded = true) }
                    _event.send(StudyResultEvent.ShowToast(error.message))
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
