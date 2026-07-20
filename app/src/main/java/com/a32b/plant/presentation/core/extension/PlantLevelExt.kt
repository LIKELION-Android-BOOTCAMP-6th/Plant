package com.a32b.plant.presentation.core.extension

import androidx.annotation.DrawableRes
import com.a32b.plant.R
import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.type.PlantLevel

val PlantLevel.resId : Int
    @DrawableRes
    get() = when(this){
        PlantLevel.LV0 -> R.drawable.ic_pot_lv0
        PlantLevel.LV1 -> R.drawable.ic_pot_lv1
        PlantLevel.LV2 -> R.drawable.ic_pot_lv2
        PlantLevel.LV3 -> R.drawable.ic_pot_lv3
        PlantLevel.LV4 -> R.drawable.ic_pot_lv4
        PlantLevel.LV5 -> R.drawable.ic_pot_lv5
        PlantLevel.LV6 -> R.drawable.ic_pot_lv6
        else -> R.drawable.logo_plant
    }