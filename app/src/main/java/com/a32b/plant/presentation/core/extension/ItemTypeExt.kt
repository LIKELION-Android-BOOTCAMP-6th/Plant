package com.a32b.plant.presentation.core.extension

import androidx.annotation.DrawableRes
import com.a32b.plant.R
import com.a32b.plant.domain.type.ItemType

/**
 아이템 이미지용 확장함수
 */
val ItemType.resId: Int
 @DrawableRes
 get() = when(this){
  ItemType.HEART -> R.drawable.ic_heart
  ItemType.SUN -> R.drawable.ic_sun
  ItemType.WATER -> R.drawable.ic_water
  ItemType.FERTILIZER -> R.drawable.ic_fertilizer
  ItemType.NUTRIENT -> R.drawable.ic_nutrient
  else -> R.drawable.ic_coin
 }