package com.a32b.plant.presentation.home.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.usecase.pot.GetActivePotUseCase
import com.a32b.plant.domain.repository.UserRepository
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActivePotUseCase: GetActivePotUseCase,
    private val getPotListUseCase: GetPotListUseCase,
    private val userRepository: UserRepository
) : BaseViewModel() {

    private val _displayPot = MutableStateFlow(Pot.EMPTY)
    val displayPot = _displayPot.asStateFlow()
    //팝업 제어용
    private val _showPotChangeDialog = MutableStateFlow(false)
    val showPotChangeDialog = _showPotChangeDialog.asStateFlow()

    // 팝업 리스트
    private val _allPots = MutableStateFlow<List<Pot>>(emptyList())
    val allPots = _allPots.asStateFlow()

    //팝업 임시 선택
    private val _tempSelectedPot = MutableStateFlow<Pot?>(null)
    val tempSelectedPot = _tempSelectedPot.asStateFlow()

    //에러 상태
    private val _isLoginError = MutableStateFlow(false)
    val isLoginError = _isLoginError.asStateFlow()

    init {
        loadActivePot()
        loadAllPots()
        observeUserData()
        checkUserAndLoadData()
    }
    private fun checkUserAndLoadData() {
        viewModelScope.launch {
            // 유저 리포지토리의 현재 유저 상태를 확인
            val user = userRepository.currentUser.value
            if (user == null || user.uid.isEmpty()) {
                _isLoginError.value = true // 로그인 실패 상태로 전환
            } else {
                loadData(user.uid)
            }
        }
    }
    private fun observeUserData() {
        viewModelScope.launch {
            userRepository.currentUser.collectLatest { user ->
                // user가 null이 아니고 uid가 있을 때만 데이터 로드 시작
                user?.let {
                    loadData(it.uid)
                }
            }
        }
    }
    private fun loadData(uid: String) {
        val lastId = userRepository.currentUser.value?.lastSelectedPotId ?: ""

        viewModelScope.launch {
            // 화분 목록 로드
            getPotListUseCase(uid).collect { _allPots.value = it }
        }

        viewModelScope.launch {
            // 현재 화분 로드
            getActivePotUseCase(uid, lastId).collect {
                _displayPot.value = it
                loaded()
            }
        }
    }
    private fun loadActivePot() {
        viewModelScope.launch {
            val uid = CurrentUser.uid
            if (uid.isEmpty()) {
                Log.e("HomeViewModel", "UID가 비어있어 데이터 로드를 건너뜁니다.")
                return@launch
            }
            val lastId = userRepository.currentUser.value?.lastSelectedPotId ?: ""
            getActivePotUseCase(CurrentUser.uid, lastId).collect {
                _displayPot.value = it
                loaded()
            }
        }
    }
    private fun loadAllPots() {
        viewModelScope.launch {
            getPotListUseCase(CurrentUser.uid).collect { _allPots.value = it }
        }
    }

    fun setTempSelectedPot(pot: Pot) {
        _tempSelectedPot.value = pot
    }

    fun confirmPotChange() {
        val selected = _tempSelectedPot.value
        if (selected != null) {
            selectPot(selected) // DB 업데이트 로직 실행
        }
        setShowPotChangeDialog(false)
        _tempSelectedPot.value = null // 초기화
    }

    fun setShowPotChangeDialog(show: Boolean) {
        if (!show) _tempSelectedPot.value = null // 닫을 때 초기화
        _showPotChangeDialog.value = show
    }
    fun selectPot(pot: Pot) {
        // 메인 화분 업데이트
        _displayPot.value = pot

        // 선택 화분으로 DB 업데이트
        viewModelScope.launch {
            userRepository.updateLastSelectedPot(CurrentUser.uid, pot.id)
        }
    }
}