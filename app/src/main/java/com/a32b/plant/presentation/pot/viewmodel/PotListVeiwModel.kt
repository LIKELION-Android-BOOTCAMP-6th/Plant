package com.a32b.plant.presentation.pot.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.a32b.plant.di.CurrentUser
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.usecase.pot.GetPotListUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PotListViewModel @Inject constructor(
    private val getPotListUseCase: GetPotListUseCase
) : ViewModel() {

    private val _pots = MutableStateFlow<List<Pot>>(emptyList())
    val pots = _pots.asStateFlow()

    init {
        viewModelScope.launch {
            getPotListUseCase(CurrentUser.uid).collect { _pots.value = it }
        }
    }
}