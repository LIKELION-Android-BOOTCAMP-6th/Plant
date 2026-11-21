package com.a32b.plant

import com.google.firebase.appcheck.AppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory

// release 빌드: Play Integrity 제공자 (#608)
// 앱 서명 인증서 SHA-256 등록 등 콘솔 설정이 끝나야 토큰이 발급된다 (설정 방법·어느 키를 넣는지는 docs/APP_CHECK.md)
internal fun appCheckProviderFactory(): AppCheckProviderFactory =
    PlayIntegrityAppCheckProviderFactory.getInstance()
