package com.a32b.plant.presentation.feedback.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.domain.error.AppError
import com.a32b.plant.domain.model.FeedbackSubmissionResult
import com.a32b.plant.domain.repository.FeedbackRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.type.ImprovementCategory
import com.a32b.plant.domain.usecase.feedback.CreateImprovementSuggestionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ImprovementSuggestionUiState(
    val category: ImprovementCategory? = null,
    val description: String = "",
    val appVersion: String = "",
    val isLoading: Boolean = false
) {
    val canSubmit: Boolean
        get() = category != null && description.isNotBlank() && !isLoading
}

sealed class ImprovementSuggestionEvent {
    data class ShowToast(val message: String) : ImprovementSuggestionEvent()
    data object NavigateBack : ImprovementSuggestionEvent()
}

@HiltViewModel
class ImprovementSuggestionViewModel @Inject constructor(
    private val createImprovementSuggestionUseCase: CreateImprovementSuggestionUseCase,
    private val feedbackRepository: FeedbackRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {
    private val feedbackId = feedbackRepository.createFeedbackId()
    private var hasSubmitted = false
    private val _uiState = MutableStateFlow(ImprovementSuggestionUiState())
    val uiState = _uiState.asStateFlow()

    private val eventChannel = Channel<ImprovementSuggestionEvent>(Channel.BUFFERED)
    val events = eventChannel.receiveAsFlow()

    fun loadAppVersion() {
        _uiState.update { it.copy(appVersion = readAppVersion(context)) }
    }

    fun onCategoryChanged(category: ImprovementCategory?) {
        _uiState.update { it.copy(category = category) }
    }

    fun onDescriptionChanged(description: String) {
        _uiState.update { it.copy(description = description) }
    }

    fun submit() {
        val state = _uiState.value
        if (hasSubmitted || !state.canSubmit) return
        val category = state.category ?: return
        _uiState.update { it.copy(isLoading = true) }

        viewModelScope.launch {
            createImprovementSuggestionUseCase(
                feedbackId = feedbackId,
                category = category,
                description = state.description,
                appVersion = state.appVersion
            ).onSuccess { result ->
                hasSubmitted = true
                _uiState.update { it.copy(isLoading = false) }
                when (result) {
                    FeedbackSubmissionResult.Saved -> {
                        eventChannel.send(ImprovementSuggestionEvent.ShowToast(SUCCESS_MESSAGE))
                        eventChannel.send(ImprovementSuggestionEvent.NavigateBack)
                    }
                    FeedbackSubmissionResult.Pending -> {
                        eventChannel.send(ImprovementSuggestionEvent.ShowToast(PENDING_MESSAGE))
                        eventChannel.send(ImprovementSuggestionEvent.NavigateBack)
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                if (error !is AppError.UnknownUser) {
                    eventChannel.send(ImprovementSuggestionEvent.ShowToast(error.message))
                }
            }
        }
    }

    private fun readAppVersion(context: Context): String {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.PackageInfoFlags.of(0)
            )
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        return packageInfo.versionName ?: UNKNOWN_APP_VERSION
    }

    companion object {
        private const val UNKNOWN_APP_VERSION = "unknown"
        private const val SUCCESS_MESSAGE = "개선사항 제안이 접수되었습니다."
        private const val PENDING_MESSAGE = "네트워크가 불안정해 제출이 지연되고 있어요. 연결되면 자동으로 전송됩니다."
    }
}
