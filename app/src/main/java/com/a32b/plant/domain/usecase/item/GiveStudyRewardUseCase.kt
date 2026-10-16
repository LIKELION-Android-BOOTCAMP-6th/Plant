package com.a32b.plant.domain.usecase.item

import com.a32b.plant.domain.model.StudyReward
import com.a32b.plant.domain.repository.ItemRepository
import com.a32b.plant.domain.result.Result
import com.a32b.plant.domain.result.map
import com.a32b.plant.domain.type.ItemType
import com.a32b.plant.domain.usecase.session.EnsureCurrentUserUseCase
import javax.inject.Inject

class GiveStudyRewardUseCase @Inject constructor(
    private val itemRepository: ItemRepository,
    private val ensureCurrentUserUseCase: EnsureCurrentUserUseCase
) {
    suspend operator fun invoke(sessionTimeMs: Long): Result<StudyReward?> {
        val reward = computeReward(sessionTimeMs) ?: return Result.Success(null)

        val user = when (val result = ensureCurrentUserUseCase()) {
            is Result.Success -> result.data
            is Result.Failure -> return Result.Failure(result.error)
        }

        val allItems = reward.items + List(reward.boxCount) { ItemType.BOX }
        return itemRepository.addDrawItems(user.uid, allItems).map { reward }
    }

    private fun computeReward(sessionTimeMs: Long): StudyReward? {
        val hours = sessionTimeMs / (1000L * 60 * 60)
        return when {
            hours >= 9 -> StudyReward(
                listOf(ItemType.HEART, ItemType.SUN, ItemType.WATER, ItemType.FERTILIZER, ItemType.NUTRIENT),
                boxCount = 5
            )
            hours >= 7 -> StudyReward(
                listOf(ItemType.HEART, ItemType.SUN, ItemType.WATER, ItemType.FERTILIZER),
                boxCount = 4
            )
            hours >= 5 -> StudyReward(
                listOf(ItemType.HEART, ItemType.SUN, ItemType.WATER),
                boxCount = 3
            )
            hours >= 3 -> StudyReward(
                listOf(ItemType.HEART, ItemType.SUN),
                boxCount = 2
            )
            hours >= 1 -> StudyReward(
                listOf(ItemType.HEART),
                boxCount = 1
            )
            else -> null
        }
    }
}
