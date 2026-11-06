package com.a32b.plant.presentation.studying.service

import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * 학습 화면(StudyingViewModel) 밖에서 학습 타이머 서비스를 다루기 위한 진입점
 */
class StudyingTimerServiceController @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 실행 중인 타이머 서비스를 멈추고 알림을 없앤다 (#514)
     * startService(ACTION_STOP)와 달리 서비스가 없으면 새로 시작하지 않고, 백그라운드 시작 제한에도 걸리지 않는다
     */
    fun stop() {
        context.stopService(Intent(context, StudyingTimerService::class.java))
    }
}
