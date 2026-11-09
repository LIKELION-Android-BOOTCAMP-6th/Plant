package com.a32b.plant.domain.usecase.pot

import com.a32b.plant.domain.model.Pot
import com.a32b.plant.domain.repository.PotRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GetActivePotUseCase @Inject constructor(
    private val repository: PotRepository
) {
    /**
     * 현재 메인 표출 화분을 반환
     * 화분 목록이 새로 방출될 때마다 그 시점의 selectedPotId로 다시 고른다 (#582)
     * - 홈에서 대표 화분을 바꾼 뒤 화분 문서가 바뀌어도 처음 읽은 화분으로 되돌아가지 않게 하기 위함
     * - selectedPotId만 바뀔 때는 방출하지 않는다. 화분 변경 시 호출하는 쪽이 표시 화분을 직접 바꾼다
     * - combine을 쓰지 않는 이유: 화분 목록 Flow가 방출 없이 끝나도(조회 실패) StateFlow는 끝나지 않아
     *   결과 Flow가 끝나지 않고, 그 뒤의 로딩 해제(#487)가 실행되지 않음
     * 선택한 화분은 진행 중인 화분 안에서만 찾고, 없으면(완료·삭제) 진행 중인 첫 화분으로 전환
     */
    operator fun invoke(uid: String, selectedPotId: StateFlow<String>): Flow<Pot> =
        repository.getPots(uid).map { pots ->
            val activePots = pots.filter { !it.isCompleted }

            if (activePots.isEmpty()) {
                return@map Pot.EMPTY
            }

            activePots.find { it.id == selectedPotId.value } ?: activePots.first()
        }
}
