package com.a32b.plant.presentation.studying.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import androidx.navigation.toRoute
import com.a32b.plant.core.navigation.Routes
import com.a32b.plant.core.util.TimeFormatter
import com.a32b.plant.domain.model.StudyLogRef
import com.a32b.plant.domain.model.StudyReward
import com.a32b.plant.domain.model.StudyRewardStatus
import com.a32b.plant.domain.repository.GalleryRepository
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.onFailure
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.presentation.studying.StudyingConstants.REWARD_DELAY_NOTICE_MS
import com.a32b.plant.presentation.studying.StudyingConstants.REWARD_MIN_STUDY_TIME_MS
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

/** 결과 화면의 보상 표시 상태 (#453) — 보상은 서버 트리거가 지급하므로 기록 문서를 구독해 표시한다 */
sealed interface StudyRewardUi {
    /** 표시하지 않음 (1시간 미만, 보상 없음, 기록이 없음) */
    data object Hidden : StudyRewardUi

    /** 서버에 저장됐고 트리거가 지급하기를 기다리는 중 */
    data object Loading : StudyRewardUi

    /** 지급이 [REWARD_DELAY_NOTICE_MS] 넘게 늦어짐 (콜드 스타트, 트리거 재시도 등) — 지급되면 홈에서도 보여줌 */
    data object Delayed : StudyRewardUi

    /** 오프라인 등으로 서버 반영을 확인하지 못함 — 연결되면 저장·지급된다 */
    data object WaitingForNetwork : StudyRewardUi

    data class Granted(val reward: StudyReward) : StudyRewardUi
}

data class StudyResultUiState(
    val isLoading: Boolean = false,
    val isDialogShow: Boolean = false,
    val isImageSaveSupported: Boolean = false,
    val reward: StudyRewardUi = StudyRewardUi.Hidden,
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
    private val studyingRepository: StudyingRepository,
    private val galleryRepository: GalleryRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase,
    savedStateHandle: SavedStateHandle
) : BaseViewModel() {

    private val route = savedStateHandle.toRoute<Routes.StudyResult>()

    private val _uiState = MutableStateFlow(
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
    )
    val uiState = _uiState.asStateFlow()

    private val _event = Channel<StudyResultEvent>(Channel.BUFFERED)
    val event = _event.receiveAsFlow()

    // 보상을 확인했다고 한 번만 표시 (구독이 같은 보상을 여러 번 보내도 쓰기를 반복하지 않게)
    private var isRewardSeenMarked = false

    init {
        // 결과 데이터는 이전 화면에서 전달받은 값이라 즉시 표시, 보상은 도착 시 표시
        // (보상은 서버가 지급하므로 오프라인에서는 연결된 뒤에 도착함)
        loaded()
        observeReward()
    }

    fun onDialogShow() = _uiState.update { it.copy(isDialogShow = true) }
    fun onDialogDismiss() = _uiState.update { it.copy(isDialogShow = false) }

    /**
     * 학습 기록(logs/{sessionId})의 보상 지급 상태를 구독한다
     * 보상을 지급하는 코드가 앱에 없으므로 화면 재진입·회전·프로세스 종료 후 복원돼도 다시 지급되지 않는다
     */
    private fun observeReward() {
        // 1시간 미만은 서버도 보상을 주지 않으므로 기다리지 않는다 (서버 티어 표의 최소 기준과 같은 표시용 값)
        if (route.time < REWARD_MIN_STUDY_TIME_MS || route.sessionId.isBlank()) return
        val uid = ensureCurrentUserUseCase { it.uid } ?: return

        _uiState.update {
            it.copy(reward = if (route.isSavePending) StudyRewardUi.WaitingForNetwork else StudyRewardUi.Loading)
        }
        val delayNoticeJob = viewModelScope.launch {
            delay(REWARD_DELAY_NOTICE_MS)
            _uiState.update { state ->
                if (state.reward == StudyRewardUi.Loading) state.copy(reward = StudyRewardUi.Delayed) else state
            }
        }
        viewModelScope.launch {
            studyingRepository.observeStudyReward(uid, route.potId, route.sessionId)
                .catch { e ->
                    Log.e("StudyResult", "보상 상태 구독 실패", e)
                    delayNoticeJob.cancel()
                    // 지급은 서버가 하므로 표시만 못 한 것 — 홈의 쌓인 보상 창에서 보여준다
                    _uiState.update { it.copy(reward = StudyRewardUi.Hidden) }
                }
                .collect { status -> onRewardStatus(uid, status, delayNoticeJob) }
        }
    }

    private fun onRewardStatus(uid: String, status: StudyRewardStatus?, delayNoticeJob: Job) {
        when (status) {
            // 대기 중이면 현재 안내(받는 중 / 늦어짐 / 연결되면 지급) 유지
            StudyRewardStatus.Pending -> Unit
            // 기록이 없음(규칙 거부로 되돌아감, 삭제) 또는 보상 없음
            null, StudyRewardStatus.None -> {
                delayNoticeJob.cancel()
                _uiState.update { it.copy(reward = StudyRewardUi.Hidden) }
            }
            is StudyRewardStatus.Granted -> {
                delayNoticeJob.cancel()
                _uiState.update { it.copy(reward = StudyRewardUi.Granted(status.reward)) }
                if (!status.isSeen) markRewardSeen(uid)
            }
        }
    }

    /** 결과 화면에서 보여준 보상은 홈의 쌓인 보상 창에 다시 나오지 않도록 확인 표시 (실패하면 홈에서 한 번 더 보여줌) */
    private fun markRewardSeen(uid: String) {
        if (isRewardSeenMarked) return
        isRewardSeenMarked = true
        viewModelScope.launch {
            studyingRepository.markStudyRewardsSeen(uid, listOf(StudyLogRef(route.potId, route.sessionId)))
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
