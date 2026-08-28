package com.a32b.plant.di.qualifier

import javax.inject.Qualifier

/** 앱 생명주기 동안 유지되는 CoroutineScope. currentUser 실시간 구독처럼
 *  개별 화면 스코프보다 오래 살아야 하는 작업에 사용한다. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
