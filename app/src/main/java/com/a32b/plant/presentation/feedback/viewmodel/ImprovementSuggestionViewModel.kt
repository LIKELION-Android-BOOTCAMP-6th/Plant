package com.a32b.plant.presentation.feedback.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.NETWORK_SLOW_MESSAGE
import com.a32b.plant.core.util.appVersionName
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

    private val _eventChannel = Channel<ImprovementSuggestionEvent>(Channel.BUFFERED)
    val events = _eventChannel.receiveAsFlow()

    fun loadAppVersion() {
        _uiState.update { it.copy(appVersion = context.appVersionName()) }
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
                        _eventChannel.send(ImprovementSuggestionEvent.ShowToast(SUCCESS_MESSAGE))
                        _eventChannel.send(ImprovementSuggestionEvent.NavigateBack)
                    }
                    FeedbackSubmissionResult.Pending -> {
                        _eventChannel.send(ImprovementSuggestionEvent.ShowToast(NETWORK_SLOW_MESSAGE))
                        _eventChannel.send(ImprovementSuggestionEvent.NavigateBack)
                    }
                }
            }.onFailure { error ->
                _uiState.update { it.copy(isLoading = false) }
                if (error !is AppError.UnknownUser) {
                    _eventChannel.send(ImprovementSuggestionEvent.ShowToast(error.message))
                }
            }
        }
    }

    companion object {
        private const val SUCCESS_MESSAGE = "개선사항 제안이 접수되었습니다."
    }
}
