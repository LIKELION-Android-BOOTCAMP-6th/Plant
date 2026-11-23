package com.a32b.plant.domain.usecase.studying

import com.a32b.plant.core.util.KST_ZONE_ID
import com.a32b.plant.domain.repository.StudyingRepository
import com.a32b.plant.domain.result.getOrNull
import java.time.LocalDate
import javax.inject.Inject

/**
 * 학습 화면 알림 설정 안내를 오늘(한국 시간) 띄워도 되는지 확인하고, 띄워도 되면 오늘 띄웠다고 기록한다 (#610)
 *
 * 하루 1번만 띄우므로 같은 날 학습 화면에 다시 들어와도(일시정지 후, 백업 복구 후 등) false
 * 읽기에 실패하면 안 띄운 것으로 본다 — 안내를 아예 못 보는 것보다 한 번 더 보는 쪽이 낫다
 * 기록에 실패해도 true — 같은 날 한 번 더 뜰 수 있을 뿐 학습에는 영향이 없다
 */
class ClaimDailyNotificationGuideUseCase @Inject constructor(
    private val repository: StudyingRepository
) {
    suspend operator fun invoke(today: LocalDate = LocalDate.now(KST_ZONE_ID)): Boolean {
        val lastShownDate = repository.readNotificationGuideShownDate().getOrNull()
        if (lastShownDate == today) return false
        repository.saveNotificationGuideShownDate(today)
        return true
    }
}
