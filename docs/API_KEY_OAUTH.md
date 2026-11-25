# Android API 키 앱 제한과 OAuth 동의 화면 (#664)

> `google-services.json`에 들어 있는 Android API 키에 **앱 제한(패키지명 + SHA-1)** 을 걸어, 다른 앱이 이 키를 쓰지 못하게 한다.
> 키 자체는 비밀이 아니다. Firebase 문서도 Firebase용 API 키는 코드나 설정 파일에 넣어도 된다고 한다. 실제 보호는 보안 규칙과 App Check([APP_CHECK.md](APP_CHECK.md))가 맡는다. 앱 제한은 콘솔 설정만으로 거는 추가 방어다.
> Google 로그인 동의 화면(Google 인증 플랫폼)이 팀원이 아닌 계정도 로그인할 수 있는 상태인지도 함께 점검한다.

## 현재 설정 (2026-11-25)

| 항목 | 적용 전 | 지금 |
|---|---|---|
| Android 키 애플리케이션 제한 | 없음 (콘솔에 "현재 이 키는 모든 애플리케이션에서 사용할 수 있습니다" 경고) | Android 앱: `com.a32b.plant` + SHA-1 7개 |
| Android 키 API 제한 | API 25개 (Firebase가 키를 만들 때 건 것으로 보임) | 그대로 유지 (아래 "API 제한" 참고) |
| OAuth 게시 상태 / 사용자 유형 | 프로덕션 / 외부 | 변경 없음 |
| OAuth 브랜딩 | 앱 이름·지원 이메일만 있고 홈페이지·처리방침·개발자 연락처가 비어 있었음 | 아래 "OAuth 동의 화면" 표의 값으로 채움. 로고 없음 |

## 등록해야 하는 SHA-1

API 키 앱 제한에는 **Firebase 프로젝트 설정에 등록된 SHA-1을 모두** 넣는다. 하나라도 빠지면 그 인증서로 서명한 빌드만 키가 막힌다.

| 종류 | 확인 위치 | 비고 |
|---|---|---|
| 팀원 각자의 debug SHA-1 | 각자 PC에서 `./gradlew :app:signingReport` → `Variant: debug`의 `SHA1` | PC(=`~/.android/debug.keystore`)마다 다르다 |
| 업로드 키 SHA-1 | Play Console의 앱 서명 화면 → 업로드 키 인증서 (#621에서 SHA를 가져온 곳) | 로컬 release 빌드(`assembleRelease`, `bundleRelease`)가 이 키로 서명된다 (#621) |
| Play 앱 서명 키 SHA-1 (화면에 나오는 것 전부) | 같은 화면 → 앱 서명 키 인증서 | Play에서 설치한 앱(내부 테스트 트랙 등)이 이 키로 서명된다 (#621). 키를 업그레이드했으면 이전 키와 지금 키가 함께 나오고, 양자 내성 키가 따로 나올 수도 있다 |

- 기준 목록은 Firebase 콘솔 → 프로젝트 설정 → 내 앱 → Android 앱(`com.a32b.plant`)의 "SHA 인증서 지문"이다. API 키 앱 제한의 SHA-1 목록을 이 목록과 1:1로 맞춘다
  - 2026-11-25 기준 SHA-1은 7개다: 팀원 debug 3개, 업로드 키 1개, 앱 서명 키 3개(이전 키, 지금 키, 양자 내성 키)
  - 앱 서명 키가 여러 개인 경우 Play가 기기마다 어느 키로 서명한 앱을 내려주는지는 확인하지 않았다 (미확인). 그래서 화면에 나온 앱 서명 키 SHA-1은 **모두** 넣는다. 빠뜨리면 그 키로 서명된 설치본이 막힐 수 있다
- `google-services.json`의 SHA-1(`oauth_client` 중 `client_type: 1` 항목의 `certificate_hash`)은 기준으로 쓰지 않는다. 2026-11-25에 확인한 로컬 파일에는 5개만 있어 Firebase 콘솔의 7개와 달랐다
- API 키 앱 제한에는 SHA-1을 넣는다. SHA-256은 App Check 등록에 쓴다 ([APP_CHECK.md](APP_CHECK.md#콘솔play-console-설정-처음-한-번))

## 앱 제한 적용 절차

1. Google Cloud 콘솔 → 프로젝트 `plant-82b09` 선택 → **API 및 서비스 → 사용자 인증 정보** → API 키 목록에서 `Android key (auto created by Firebase)`를 연다
   - 목록에는 `Browser key (auto created by Firebase)`도 있다. 앱이 쓰는 키는 Android 키다. 헷갈리면 "키 표시"로 키 끝자리를 `google-services.json`의 `api_key.current_key` 끝자리와 비교한다 (키 값은 저장소·채팅에 붙이지 않는다)
2. 바꾸기 전 상태를 기록한다 (애플리케이션 제한사항, API 제한사항)
3. **애플리케이션 제한사항** → "Android 앱" 선택 → 패키지 이름 `com.a32b.plant` + SHA-1 한 쌍을 위 표의 SHA-1마다 하나씩 추가한다
   - SHA-1은 Firebase 콘솔에 보이는 값(`AA:BB:…` 형식)을 그대로 복사한다
4. **API 제한사항**은 건드리지 않는다
5. 저장한다. 콘솔 안내 문구 기준으로 적용까지 **최대 5분**이 걸릴 수 있으므로, 5분 뒤에 확인한다
6. 아래 "확인할 것" 표대로 확인한다

## 확인할 것

| 케이스 | 확인 방법 | 기대 동작 |
|---|---|---|
| 팀원 각자의 디버그 빌드 | 팀원마다 디버그 빌드로 실행 | 로그인·Firestore 읽기·쓰기 정상 |
| 로컬 릴리스 빌드 (업로드 키 서명) | 아래 "로컬 릴리스 설치본 만들기"로 설치해 실행 | 정상 |
| 내부 테스트 트랙 설치본 (Play 앱 서명 키) | 내부 테스트 링크(또는 Play 스토어 앱 페이지)로 설치해 실행 | 정상 |
| 팀원이 아닌 Google 계정 | 처음 쓰는 계정으로 로그인 | 로그인 성공, 닉네임 설정 화면으로 이동 |
| 팀원 SHA-1 하나가 빠짐 | — | 그 팀원 빌드만 로그인·Firestore 요청 실패. Logcat 오류 문구는 미확인 (#664 이슈는 `API_KEY_ANDROID_APP_BLOCKED`류로 예상). SHA-1을 추가하면 해결 |

- 서명이 다른 빌드는 같은 기기에 덮어 설치할 수 없다. 빌드를 바꿀 때마다 앱을 지우고 설치한다 (기기의 로그인 상태·로컬 데이터가 지워진다)
- 로그인은 이미 로그인된 상태가 아니라 **로그아웃 → 로그인**으로 확인한다. 쓰기는 서버에 붙지 않으면 실패하는 동작(예: 게시글 좋아요, 트랜잭션)으로 확인한다
- 설치본의 서명은 `apksigner verify --print-certs <apk>`의 `certificate SHA-1 digest`로 확인할 수 있다

### 로컬 릴리스 설치본 만들기

1. 프로젝트 루트 `local.properties`에 `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`를 넣는다 (#621)
   - 경로는 `/`로 쓴다. 이 파일에서 `\`는 특수문자라 `C:\...`로 쓰면 경로가 틀어져 "파일 없음" 경고가 난다
   - 별칭은 `keytool -list -keystore <키 파일>`의 `PrivateKeyEntry` 줄 맨 앞 값이다
2. `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (이름에 `-unsigned`가 붙으면 서명 값이 덜 들어간 것)
3. 기기에서 앱을 지운 뒤 `adb install app/build/outputs/apk/release/app-release.apk`

- 이 설치본은 Play에서 설치한 것이 아니라서 Play Integrity를 통과하지 못한다. 2026-11-25 확인 때 Logcat에 `Error getting App Check token; using placeholder token instead. ... 403 body: App attestation failed.`가 나왔다. App Check 강제가 꺼져 있어 동작에는 영향이 없었지만, **강제가 켜지면 이 설치본은 서버 요청이 거부될 것**으로 보인다 ([APP_CHECK.md](APP_CHECK.md#콘솔play-console-설정-처음-한-번)). 키 제한을 다시 확인할 일이 있으면 강제 전에 하거나, 실패 원인이 키 제한인지 App Check인지 Logcat으로 나눠 본다

### 2026-11-25 확인 결과

앱 제한 적용 후, 같은 실기기(SM-N986N)에서 확인했다.

| 빌드 | 설치본 서명 SHA-1 (앞 4바이트) | 로그인 | 읽기 | 쓰기 |
|---|---|---|---|---|
| 디버그 (PC 1대) | `2A:F4:10:84` | 됨 | 됨 | 됨 (좋아요 → 취소) |
| 내부 테스트 트랙 (versionCode 1) | `74:A7:58:34` | 됨 | 됨 | 됨 |
| 로컬 릴리스 | `E4:B0:17:A8` | 됨 | 됨 | 됨 |
| 로컬 릴리스, 처음 쓰는 계정 | `E4:B0:17:A8` | 됨 (닉네임 설정 화면까지 이동) | — | — |

- 세 빌드 모두 앱 Logcat에 `API key`, `blocked`, `PERMISSION_DENIED`, `API_KEY` 문구가 없었다
- 나머지 팀원 2명의 디버그 빌드는 팀원이 직접 확인했다 (문제없음)

## API 제한

**Firebase가 만든 25개 API 제한을 그대로 둔다.**

- 앱 제한을 걸기 전부터 Android 키에는 아래 25개 API로 제한이 걸려 있었다. Firebase 문서는 Firebase가 자동으로 만든 키를 Firebase 관련 API로 제한한다고 적는다 (<https://firebase.google.com/docs/projects/api-keys>)

  Cloud Datastore API, Cloud Firestore API, Cloud Logging API, Cloud SQL Admin API, Cloud Storage for Firebase API, FCM Registration API, Firebase AI Logic API, Firebase App Check API, Firebase App Distribution API, Firebase App Hosting API, Firebase App Testers API, Firebase Hosting API, Firebase In-App Messaging API, Firebase Installations API, Firebase Management API, Firebase ML API, Firebase Phone Number Verification API, Firebase Realtime Database Management API, Firebase Remote Config API, Firebase Remote Config Realtime API, Firebase Rules API, Firebase SQL Connect API, Identity Toolkit API, ML Kit API, Token Service API

- 이 상태로 위 "확인할 것"이 모두 통과했으므로, 앱에 필요한 API는 목록 안에 있다
- 목록에는 이 앱이 쓰지 않는 API(예: Cloud SQL Admin, Firebase AI Logic, Phone Number Verification)도 있다. 줄이지 않은 이유:
  - 앱 제한을 건 뒤에는 다른 앱이 이 키를 쓰지 못하므로, 목록을 줄여서 얻는 방어가 작다
  - Firebase 문서는 Firebase용 키에서 **Firebase에 필요한 API를 빼지 않도록 조심하라**고 한다. 지표만 보고 줄이면 드물게 쓰는 기능(예: Crashlytics 보고, 토큰 갱신)이 조용히 깨질 수 있다
- 나중에 줄이려면: Cloud 콘솔 → **API 및 서비스 → 사용 설정된 API 및 서비스**에서 며칠 동안 API별 요청을 보고, 요청이 있는 API와 Firebase 문서의 "Firebase에 필요한 API" 표를 대조한 뒤 줄인다. 줄인 뒤 "확인할 것"을 다시 하고, 문제가 생기면 **API 제한부터 되돌린다**

## OAuth 동의 화면 (Google 인증 플랫폼)

Cloud 콘솔 → API 및 서비스 → **OAuth 동의 화면**을 누르면 Google 인증 플랫폼(개요, 브랜딩, 대상, 클라이언트, 데이터 액세스, 인증 센터, 설정)으로 이동한다.

| 항목 | 위치 | 값 (2026-11-25) |
|---|---|---|
| 게시 상태 | 대상 | **프로덕션 단계** |
| 사용자 유형 | 대상 | 외부 |
| 앱 이름 | 브랜딩 | `Plant` |
| 사용자 지원 이메일 | 브랜딩 | 입력돼 있음 |
| 로고 | 브랜딩 | **넣지 않음** |
| 애플리케이션 홈페이지 | 브랜딩 | `https://plant-82b09.web.app/` |
| 개인정보처리방침 링크 | 브랜딩 | `https://plant-82b09.web.app/privacy-policy.html` ([LEGAL_HOSTING.md](LEGAL_HOSTING.md)) |
| 서비스 약관 링크 | 브랜딩 | `https://plant-82b09.web.app/terms.html` |
| 승인된 도메인 | 브랜딩 | `plant-82b09.firebaseapp.com`, `plant-82b09.web.app` |
| 개발자 연락처 이메일 | 브랜딩 | 입력함 |
| 범위 | 데이터 액세스 | 등록된 범위 없음 (민감하지 않은 범위·민감한 범위·제한된 범위 모두 비어 있음) |
| OAuth 클라이언트 | 클라이언트 | Android 13개 + 웹 1개 (아래 "따로 처리할 것" 참고) |

- 로고를 넣지 않는 이유: 브랜딩 화면 안내에 따르면 게시 상태가 "테스트 중"이 아닐 때 로고를 올리면 인증을 위해 앱을 제출해야 한다. 로고가 없어도 Google 계정 선택 화면에 "Plant 계정으로 계속"이 표시되고 로그인이 된다 (2026-11-25 확인)
- 브랜딩 화면 오른쪽 "인증 상태"에 "브랜딩을 인증해야 사용자에게 브랜딩이 표시될 수 있습니다"가 떠 있다. 이번 범위에서는 브랜딩 인증을 진행하지 않았다
- "테스트" 상태에서는 등록한 테스트 사용자(최대 100명)만 쓸 수 있고 승인이 7일 뒤 만료된다. 다만 Google 문서는 이름·이메일·프로필 범위만 요청하는 앱은 예외라고 적는다 (<https://support.google.com/cloud/answer/15549945>). 이 앱은 "프로덕션"이라 해당하지 않는다
- 홈페이지·처리방침·약관 링크가 `web.app` 주소라 승인된 도메인에 `plant-82b09.web.app`을 추가했다
- 홈페이지부터 개발자 연락처까지의 값은 2026-11-25에 콘솔에서 채운 값이다. 저장 후 화면은 따로 캡처하지 않았다. 세 링크 주소는 같은 날 HTTP 200으로 열리는 것을 확인했다

## 따로 처리할 것

- **개요 → 프로젝트 진단 → 앱 보안 "보안 흐름 사용" 경고**: "다음 Android 클라이언트의 앱 소유권이 확인되지 않았습니다"라며 Android 클라이언트 13개 전부를 가리킨다. 처리 방법은 확인하지 않았다 (미확인)
- **Android OAuth 클라이언트 13개 정리**: 지금 SHA-1은 7개인데 Android 클라이언트는 13개다. 예전 팀원·강사 SHA-1의 클라이언트가 남은 것으로 보인다 (미확인). 지우면 로그인이 깨질 수 있으므로 클라이언트마다 SHA-1을 대조한 뒤 정리하고, 그다음 남은 클라이언트의 소유권 확인을 한다

## 새 팀원·새 PC가 생기면

debug SHA-1은 PC마다 다르므로, 아래 세 곳에 모두 추가해야 그 PC의 디버그 빌드가 동작한다.

1. `./gradlew :app:signingReport` → `Variant: debug`의 `SHA1` 확인
2. Firebase 콘솔 → 프로젝트 설정 → Android 앱 → SHA 인증서 지문에 추가 → 새 `google-services.json`을 받아 팀원 PC와 CI 시크릿 `GOOGLE_SERVICES_JSON`을 갱신
3. Cloud 콘솔 → 사용자 인증 정보 → Android 키 → 애플리케이션 제한사항에 `com.a32b.plant` + 같은 SHA-1 추가 (**빠뜨리면 그 PC 디버그 빌드만 키가 막힌다**)
4. App Check 디버그 토큰 등록 ([APP_CHECK.md](APP_CHECK.md#디버그-토큰-등록-팀원마다-기기마다)) — 이건 SHA-1이 아니라 기기마다 한다

팀원이 빠지면 같은 순서로 Firebase SHA-1, 키 제한 SHA-1을 지운다.

## 되돌리기

- 키 제한은 Cloud 콘솔에서 바로 되돌릴 수 있다. 문제가 생기면 애플리케이션 제한사항을 **없음**으로 돌리고(API 제한을 줄였다면 그것부터) 원인을 확인한다. 되돌릴 때도 적용까지 최대 5분이 걸릴 수 있다

## CI·로컬 에뮬레이터 영향

- PR CI의 `Android Build Check`(`.github/workflows/android-build.yml`)는 `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest`만 돌린다. 앱을 기기에서 실행하지 않으므로 API 키로 요청하지 않는다
- 규칙 테스트(`rules-test`)와 함수 테스트(`functions/test`)는 프로젝트 ID `demo-plant`로 에뮬레이터에만 접속하고 `google-services.json`을 읽지 않는다. 그래서 이 키를 쓰지 않는다
- 앱은 에뮬레이터에 연결하지 않는다 ([FIREBASE_EMULATOR.md](FIREBASE_EMULATOR.md))

## 공식 문서

- API 키 앱 제한: <https://cloud.google.com/docs/authentication/api-keys#adding-application-restrictions>
- Firebase API 키와 필요한 API 목록: <https://firebase.google.com/docs/projects/api-keys>
- OAuth 동의 화면 대상·게시 상태: <https://support.google.com/cloud/answer/15549945>
