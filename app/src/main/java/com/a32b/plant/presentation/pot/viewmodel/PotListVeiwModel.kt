package com.a32b.plant.presentation.pot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val _studyingPots = MutableStateFlow<List<Pot>>(emptyList())
    val studyingPots = _studyingPots.asStateFlow()

    // 공부 완료된 화분 리스트
    private val _completedPots = MutableStateFlow<List<Pot>>(emptyList())
    val completedPots = _completedPots.asStateFlow()


    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            safeRunCatching {
                ensureCurrentUserUseCase { user ->
                    val uid = user.uid

                    launch {
                        getPotListUseCase.getStudyingPots(uid).collect { _studyingPots.value = it }
                    }
                    launch { getPotListUseCase.getCompletedPots(uid).collect { _completedPots.value = it } }

                    loadData()
                }
            }.onFailure {
                //에러 처리
            }
        }
    }
}