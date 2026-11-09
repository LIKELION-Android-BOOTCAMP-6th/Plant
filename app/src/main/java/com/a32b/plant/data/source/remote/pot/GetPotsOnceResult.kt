package com.a32b.plant.data.source.remote.pot

import com.a32b.plant.data.model.PotDto

data class GetPotsOnceResult(val pots: List<PotDto>, val isFromCache: Boolean)
