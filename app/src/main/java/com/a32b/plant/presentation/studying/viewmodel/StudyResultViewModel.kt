package com.a32b.plant.presentation.studying.viewmodel

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.StudyReward
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.item.GiveStudyRewardUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val _uiState = MutableStateFlow(
        savedStateHandle.toRoute<Routes.StudyResult>().let { route ->
            StudyResultUiState(
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

    fun saveBitmapToGallery(context: Context, bitmap: Bitmap) {
        val resolver = context.contentResolver
        val fileName = "plant_${TimeFormatter.formatToDateOnly(LocalDateTime.now())}.png"
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/png")
            put(MediaStore.Images.Media.RELATIVE_PATH, Environment.DIRECTORY_PICTURES)
            // 쓰는 도중에는 갤러리에 노출되지 않도록 숨김 (완료되지 못한 항목은 시스템이 정리)
            put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val isSaved = withContext(Dispatchers.IO) {
                var uri: Uri? = null
                safeRunCatching {
                    val insertedUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                        ?: error("갤러리 항목 생성 실패")
                    uri = insertedUri
                    val stream = resolver.openOutputStream(insertedUri) ?: error("출력 스트림 열기 실패")
                    val isWritten = stream.use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    if (!isWritten) error("PNG 쓰기 실패")
                    // 쓰기가 끝나면 숨김 해제
                    val pendingCleared = ContentValues().apply { put(MediaStore.Images.Media.IS_PENDING, 0) }
                    if (resolver.update(insertedUri, pendingCleared, null, null) <= 0) error("숨김 해제 실패")
                }.onFailure { e ->
                    Log.e("StudyResultVM", "이미지 저장 실패: ${e.message}", e)
                    // 실패하면 갤러리에 빈 이미지 항목이 남지 않도록 삭제
                    uri?.let { safeRunCatching { resolver.delete(it, null, null) } }
                }.isSuccess
            }
            _uiState.update { it.copy(isLoading = false) }
            _event.send(StudyResultEvent.ShowToast(if (isSaved) "저장이 완료됐습니다!" else "이미지 저장에 실패했습니다."))
        }
    }
}
