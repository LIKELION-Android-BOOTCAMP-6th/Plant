package com.a32b.plant.domain.usecase.report

import com.a32b.plant.domain.model.ReportPotComparison
import com.a32b.plant.domain.model.ReportPotRank
import com.a32b.plant.domain.model.ReportRecord
import javax.inject.Inject

class CalculatePotComparisonUseCase @Inject constructor() {
    operator fun invoke(records: List<ReportRecord>): ReportPotComparison {
        val totals = records.groupBy { it.potId }.map { (potId, potRecords) ->
            PotTotal(
                potId = potId,
                potName = potRecords.first().potName,
                totalMillis = potRecords.sumOf { it.studyingTime }
            )
        }
        if (totals.isEmpty()) return ReportPotComparison(highest = null, lowest = null)

        val highestTotal = totals.maxOf { it.totalMillis }
        val highest = rankForTotal(totals, highestTotal)
        val lowest =
            if (totals.size == 1) null else rankForTotal(totals, totals.minOf { it.totalMillis })
        return ReportPotComparison(highest = highest, lowest = lowest)
    }
}

private fun rankForTotal(totals: List<PotTotal>, target: Long): ReportPotRank {
    val tied = totals.asSequence()
        .filter { it.totalMillis == target }
        .sortedWith(compareBy<PotTotal>({ it.potName }, { it.potId }))
        .toList()
    val representative = tied.first()
    return ReportPotRank(
        potId = representative.potId,
        potName = representative.potName,
        totalMillis = target,
        otherTiedCount = tied.size - 1
    )
}

private data class PotTotal(val potId: String, val potName: String, val totalMillis: Long)
