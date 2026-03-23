package com.a32b.plant.ui.feature.studying.viewmodel

import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import com.a32b.plant.ui.theme.primary
import com.a32b.plant.ui.theme.sub1
import com.a32b.plant.ui.theme.sub2

class StudyingViewModel: ViewModel() {
    private var isStudying by mutableStateOf(true)

    val buttonText: String
        get() = if(isStudying) "일시정지" else "학습하기"
    val buttonBack: Color
        get() = if(isStudying) sub2 else primary

    fun setTimer(){

    }

    fun toggleStudyStatus(){
        isStudying = !isStudying

        //타이머 일시 정지 하는 거
    }

    fun showStudyLogEdit(){

    }
}