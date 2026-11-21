package com.a32b.plant

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory

// debug 빌드: 디버그 제공자 (#608)
// 처음 실행하면 logcat에 디버그 토큰이 찍힌다. 이 토큰을 콘솔 App Check에 등록해야 강제 후에도 요청이 통과한다
// 등록 방법은 docs/APP_CHECK.md
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    DebugAppCheckProviderFactory.getInstance()
