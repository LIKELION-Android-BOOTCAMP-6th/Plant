package com.a32b.plant

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

// release 빌드: Play Integrity 제공자 (#608)
// Play 앱 서명 키 SHA-256이 콘솔에 등록되어 있어야 토큰이 발급된다
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    PlayIntegrityAppCheckProviderFactory.getInstance()
