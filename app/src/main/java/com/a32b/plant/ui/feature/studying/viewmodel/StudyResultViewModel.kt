package com.a32b.plant.ui.feature.studying.viewmodel

import androidx.lifecycle.ViewModel
import com.a32b.plant.data.repository.PotRepository

class StudyResultViewModel(
    potRepository: PotRepository,
    timestamp: String,
    tag: String,
    title: String,
    log: List<String>,
    level: String
) : ViewModel(){
}