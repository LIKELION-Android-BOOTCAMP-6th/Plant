package com.a32b.plant.presentation.pot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PotListViewModel @Inject constructor(
    private val getPotListUseCase: GetPotListUseCase,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) : ViewModel() {


    // 전체 화분 리스트
    private val _pots = MutableStateFlow<List<Pot>>(emptyList())
    val pots = _pots.asStateFlow()

    // 공부 중인 화분 리스트
   val studyingPots: StateFlow<List<Pot>> = _pots
        .map { pots -> pots.filter { !it.isCompleted } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    // 공부 완료된 화분 리스트
    val completedPots: StateFlow<List<Pot>> = _pots
        .map { pots -> pots.filter { it.isCompleted } }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )


    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            safeRunCatching {
                ensureCurrentUserUseCase { user ->
                    val uid = user.uid

                    getPotListUseCase(uid).collect { allPots ->
                        _pots.value = allPots
                    }
                }
            }.onFailure {
                //에러 처리
            }
        }
    }
}