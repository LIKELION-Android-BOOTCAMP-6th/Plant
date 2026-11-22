package com.a32b.plant.domain.usecase.appversion

import com.a32b.plant.domain.model.AppVersionCheckResult
import com.a32b.plant.domain.repository.AppConfigRepository
import com.a32b.plant.domain.result.Result
import javax.inject.Inject

/**
 * 현재 앱 버전이 최소 지원 버전보다 낮은지 확인한다 (#662)
 *
 * 확인할 수 없으면(읽기 실패·문서 없음·값 없음·잘못된 값) 통과시킨다.
 * 오프라인 우선 원칙: 버전 확인 때문에 앱을 못 쓰게 되는 일은 값이 확실히 낮다고 나온 경우만으로 한정한다.
 */
class CheckAppVersionUseCase @Inject constructor(
    private val appConfigRepository: AppConfigRepository
) {
    suspend operator fun invoke(currentVersionCode: Long): AppVersionCheckResult {
        val policy = when (val result = appConfigRepository.getAppVersionPolicy()) {
            is Result.Success -> result.data
            is Result.Failure -> return AppVersionCheckResult.UpToDate
        }
        val minVersionCode = policy?.minVersionCode ?: return AppVersionCheckResult.UpToDate

        return if (currentVersionCode < minVersionCode) {
            AppVersionCheckResult.UpdateRequired(policy.message)
        } else {
            AppVersionCheckResult.UpToDate
        }
    }
}
