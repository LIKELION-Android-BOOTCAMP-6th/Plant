package com.a32b.plant.presentation.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.core.util.safeRunCatching
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.usecase.pot.GetActivePotUseCase
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActivePotUseCase: GetActivePotUseCase,
    private val getPotListUseCase: GetPotListUseCase,
    private val userRepository: UserRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
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

    //유저 이름 상태
    private val _userName = MutableStateFlow("")
    val userName = _userName.asStateFlow()

    init {
        checkUserAndLoadData()
    }
    private fun checkUserAndLoadData() {
        viewModelScope.launch {
            ensureCurrentUserUseCase { user ->
                _userName.value = user.nickname

                loadActivePot(uid = user.uid, lastSelectedPotId = user.lastSelectedPotId)
                loadStudyingPots(uid = user.uid)
                loaded()
            } ?: run {
                _isLoginError.value = true
            }
        }
    }

    private fun loadActivePot(uid: String, lastSelectedPotId: String){
        viewModelScope.launch {
            safeRunCatching {
                getActivePotUseCase(uid = uid, lastSelectedPotId = lastSelectedPotId)
                    .collect { pot ->
                        _displayPot.value = pot
                    }
            }.onFailure { throwable ->
                // 필요시 에러 핸들링
            }
        }
    }

    private fun loadStudyingPots(uid: String){
        viewModelScope.launch {
            safeRunCatching {
                getPotListUseCase.getStudyingPots(uid = uid)
                    .collect { pots ->
                        _allPots.value = pots
                    }
            }.onFailure { throwable ->
                // 필요시 에러 핸들링
            }
        }
    }
    fun setTempSelectedPot(pot: Pot) {
        _tempSelectedPot.value = pot
    }

    fun confirmPotChange() {
        _tempSelectedPot.value?.let { selected ->
            _displayPot.value = selected

            viewModelScope.launch {
                safeRunCatching {
                    ensureCurrentUserUseCase { user ->
                        userRepository.updateLastSelectedPot(user.uid, selected.id)
                    }
                }.onFailure {
                    // 예외 처리
                }
            }
        }
        setShowPotChangeDialog(false)
    }

    fun setShowPotChangeDialog(show: Boolean) {
        if (!show) _tempSelectedPot.value = null // 닫을 때 초기화
        else _tempSelectedPot.value = _displayPot.value

        _showPotChangeDialog.value = show
    }

}