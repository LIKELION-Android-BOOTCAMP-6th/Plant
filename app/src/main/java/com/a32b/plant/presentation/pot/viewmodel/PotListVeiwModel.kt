package com.a32b.plant.presentation.pot.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.PotRepository
import com.a32b.plant.domain.result.onSuccess
import com.a32b.plant.domain.type.PotSortType
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.util.sortedByType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PotListViewModel @Inject constructor(
    private val getPotListUseCase: GetPotListUseCase,
    private val potRepository: PotRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) : BaseViewModel() {


    // 전체 화분 리스트
    private val _pots = MutableStateFlow<List<Pot>>(emptyList())
    val pots = _pots.asStateFlow()

    // 정렬 — 기기에 저장된 값을 loadData()에서 읽어 덮어쓴다 (#803)
    private val _sortType = MutableStateFlow(PotSortType.DEFAULT)
    val sortType: StateFlow<PotSortType> = _sortType.asStateFlow()

    fun setSortType(sortType: PotSortType) {
        if (_sortType.value == sortType) return
        _sortType.value = sortType
        // 저장에 실패해도 이번 실행 중에는 고른 정렬을 그대로 보여 준다 (토스트 없음, 로그는 Repository에서)
        viewModelScope.launch { potRepository.saveSortType(sortType) }
    }

    // 공부 중인 화분 리스트
   val studyingPots: StateFlow<List<Pot>> = combine(_pots, _sortType) { pots, sortType ->
       pots.filter { !it.isCompleted }.sortedByType(sortType)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // 공부 완료된 화분 리스트
    val completedPots: StateFlow<List<Pot>> = combine(_pots, _sortType) { pots, sortType ->
        pots.filter { it.isCompleted }.sortedByType(sortType)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )


    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            // 저장된 정렬을 목록보다 먼저 읽는다 — 로딩(LoadableScreen)이 풀릴 때 "생성일" 순서가 잠깐 보이지 않게 (#803)
            // 읽기에 실패하면 기본값("생성일") 그대로
            potRepository.readSortType().onSuccess { _sortType.value = it }

            safeRunCatching {
                ensureCurrentUserUseCase { user ->
                    val uid = user.uid

                    getPotListUseCase(uid).collect { allPots ->
                        _pots.value = allPots
                        loaded() // BaseViewModel: 최초 방출 시 LoadableScreen 해제 (#384)
                    }
                }
            }
            // 세션 만료(유저 없음)나 구독 실패로 방출 없이 끝난 경우에도 로딩 해제 → 빈 목록 표시
            loaded()
        }
    }
}