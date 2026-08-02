package com.a32b.plant.presentation.home.viewmodel

import androidx.lifecycle.viewModelScope
import com.a32b.plant.core.base.BaseViewModel
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.usecase.pot.GetActivePotUseCase
import com.a32b.plant.domain.usecase.pot.GetActivePotsUseCase
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import com.a32b.plant.domain.repository.UserRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val getActivePotUseCase: GetActivePotUseCase,
    private val getActivePotsUseCase: GetActivePotsUseCase,
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
            // 유저 리포지토리의 현재 유저 상태를 확인
            ensureCurrentUserUseCase { user ->
                _userName.value = user.nickname

                loadActivePot(uid = user.uid, lastSelectedPotId = user.lastSelectedPotId)
                loadActivePots(uid = user.uid)
                loaded()
            } ?: run {
                _isLoginError.value = true
            }
        }
    }

    private fun loadActivePot(uid: String, lastSelectedPotId: String){
        viewModelScope.launch {
            getActivePotUseCase(uid = uid, lastSelectedPotId = lastSelectedPotId)
                .catch { throwable -> }
                .collect { pot ->
                    _displayPot.value = pot
                }
        }
    }
    private fun loadActivePots(uid: String) {
        viewModelScope.launch {
            getActivePotsUseCase(uid = uid)
                .catch { throwable -> }
                .collect { pots ->
                    _allPots.value = pots
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
                ensureCurrentUserUseCase { user->
                    userRepository.updateLastSelectedPot(user.uid, selected.id)
                }
            }
        }
        setShowPotChangeDialog(false)
    }

    fun setShowPotChangeDialog(show: Boolean) {
        if (!show){
            // 닫을 때 초기화
            _tempSelectedPot.value = null
        } else {
            _tempSelectedPot.value = _displayPot.value
        }
        _showPotChangeDialog.value = show
    }
}