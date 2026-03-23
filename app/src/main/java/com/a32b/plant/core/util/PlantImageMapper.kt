package com.a32b.plant.core.util

import com.a32b.plant.R

object PlantImageMapper {
    fun getPlantImage(level: String): Int{
        return when(level){
            "lv.0" -> R.drawable.ic_pot_lv0
            "lv.1" -> R.drawable.ic_pot_lv1
            "lv.2" -> R.drawable.ic_pot_lv2
            "lv.3" -> R.drawable.ic_pot_lv3
            "lv.4" -> R.drawable.ic_pot_lv4
            "lv.5" -> R.drawable.ic_pot_lv5
            "lv.6" -> R.drawable.ic_pot_lv6
            else -> R.drawable.logo_plant
        }
    }
}