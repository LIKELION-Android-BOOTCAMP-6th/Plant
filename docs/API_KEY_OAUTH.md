# Android API 키 앱 제한과 OAuth 동의 화면 (#664)

> `google-services.json`에 들어 있는 Android API 키에 **앱 제한(패키지명 + SHA-1)** 을 건다. 등록하지 않은 서명의 빌드에서 Firebase Installations 요청이 `403 PERMISSION_DENIED`로 거부되는 것을 확인했다 (아래 "확인 결과"의 반대 확인. 키 제한 때문으로 본다. 다른 Firebase 요청은 미확인). 다만 Cloud 문서 기준으로 Android 앱 제한은 요청 헤더로 판정해 우회가 쉬우므로, 앱 밖 요청을 막는 장치로 보지 않는다.
> 키 자체는 비밀이 아니다. Firebase 문서도 키를 Firebase 관련 API로 제한하고 보안 규칙·App Check로 보호하는 구성을 전제로, Firebase용 API 키는 코드나 설정 파일에 넣어도 된다고 한다. 실제 보호는 보안 규칙이 맡고, App Check([APP_CHECK.md](APP_CHECK.md))는 강제를 켠 뒤부터 Functions·Firestore 요청을 함께 막는다 (2026-11-26 기준 강제는 꺼져 있음, #608). 앱 제한은 콘솔 설정만으로 거는 추가 방어다.
> Google 로그인 동의 화면(Google 인증 플랫폼)이 팀원이 아닌 계정도 로그인할 수 있는 상태인지도 함께 점검한다.

## 현재 설정 (2026-11-25)

| 항목 | 적용 전 | 지금 |
|---|---|---|
| Android 키 애플리케이션 제한 | 없음 (콘솔에 "현재 이 키는 모든 애플리케이션에서 사용할 수 있습니다" 경고) | Android 앱: `com.a32b.plant` + SHA-1 7개 (콘솔에 입력한 값 기준. 저장 후 화면은 캡처하지 않았고, 제한이 걸린 것은 반대 확인으로 확인) |
| Android 키 API 제한 | API 25개 (Firebase가 건 것으로 보임. 언제 걸었는지는 미확인) | 그대로 유지 (아래 "API 제한" 참고) |
| OAuth 게시 상태 / 사용자 유형 | 프로덕션 / 외부 | 변경 없음 |
| OAuth 브랜딩 | 앱 이름·지원 이메일만 있고 홈페이지·처리방침·약관 링크와 개발자 연락처가 비어 있었음. 승인된 도메인은 `plant-82b09.firebaseapp.com`뿐이었음 | 아래 "OAuth 동의 화면" 표의 값으로 채움. 로고 없음 |

## 등록해야 하는 SHA-1

API 키 앱 제한에는 **Firebase 프로젝트 설정에 등록된 SHA-1을 모두** 넣는다. 하나라도 빠지면 그 인증서로 서명한 빌드만 키가 막힌다.

| 종류 | 확인 위치 | 비고 |
|---|---|---|
| 팀원 각자의 debug SHA-1 | 각자 PC에서 `./gradlew :app:signingReport` → `Variant: debug`의 `SHA1` | PC(=`~/.android/debug.keystore`)마다 다르다 |
| 업로드 키 SHA-1 | Play Console의 앱 서명 화면 → 업로드 키 인증서 (#621에서 SHA를 가져온 곳) | 로컬 release 빌드(`assembleRelease`, `bundleRelease`)가 이 키로 서명된다 (#621). 서명 값 4개(`local.properties`, 없으면 같은 이름의 환경변수)가 모두 있고 `RELEASE_STORE_FILE` 파일이 실제로 있을 때만 서명되고, 아니면 서명 없는 결과물이 나온다 (`app/build.gradle.kts`) |
| Play 앱 서명 키 SHA-1 (화면에 나오는 것 전부) | 같은 화면 → 앱 서명 키 인증서 | Play에서 설치한 앱(내부 테스트 트랙 등)이 이 키로 서명된다 (#621. 내부 앱 공유 설치본은 제외, 아래 "새 팀원·새 PC가 생기면" 참고). 키를 업그레이드했으면 이전 키와 지금 키가 함께 나오고, 양자 내성 키가 따로 나올 수도 있다 |

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
| 팀원 각자의 디버그 빌드 | 팀원마다 디버그 빌드로 실행 (키 제한은 표 아래의 Logcat 확인으로 본다) | 로그인·Firestore 읽기·쓰기 정상 |
| 로컬 릴리스 빌드 (업로드 키 서명) | 아래 "로컬 릴리스 설치본 만들기"로 설치해 실행 | 정상 |
| 내부 테스트 트랙 설치본 (Play 앱 서명 키) | 내부 테스트 링크(또는 Play 스토어 앱 페이지)로 설치해 실행 | 정상 |
| 팀원이 아닌 Google 계정 | 처음 쓰는 계정으로 로그인 | 로그인 성공, 닉네임 설정 화면으로 이동 |
| 등록되지 않은 SHA-1로 서명한 빌드 | 아래 "확인 결과"의 반대 확인 | 그 빌드만 실패. 키 제한에 없으면 Firebase Installations 요청이 `403 PERMISSION_DENIED`를 받았다 (다른 Firebase 요청은 미확인). 그 SHA-1의 Android OAuth 클라이언트도 없으면 Google 로그인은 Firebase 로그인 요청 전 단계에서 `UNREGISTERED_ON_API_CONSOLE`로 실패했다 (반대 확인은 Firebase·키 제한 둘 다 없는 경우만 봤다). Firebase와 키 제한에 SHA-1을 추가하면 해결될 것으로 보인다 |

- 서명이 다른 빌드는 같은 기기에 덮어 설치할 수 없다. 빌드를 바꿀 때마다 앱을 지우고 설치한다 (기기의 로그인 상태·로컬 데이터가 지워진다)
- 로그인은 이미 로그인된 상태가 아니라 **로그아웃 → 로그인**으로 확인한다. 쓰기는 서버에 붙지 않으면 실패하는 동작(예: 게시글 좋아요. 트랜잭션으로 쓴다)으로 확인한다. 이건 앱이 정상인지 보는 확인이다. Firestore 요청에는 이 키가 실리지 않으므로(firebase-firestore 26.1.1 소스에 API 키를 붙이는 코드가 없음), 키 제한에 막혔는지는 앱을 지우고 새로 설치한 직후 Logcat의 Firebase Installations `403 PERMISSION_DENIED`로 본다 (App Check의 `403 ... App attestation failed`와는 다른 오류다. 반대 확인의 403도 새로 설치한 앱을 켜자마자 나왔고, 이미 설치된 앱에서도 같은 요청이 나가는지는 미확인). 로그인 실패는 키가 아니라 OAuth 클라이언트 문제일 수도 있다 (위 표의 마지막 행)
- 설치본의 서명은 `apksigner verify --print-certs <apk>`의 `certificate SHA-1 digest`로 확인할 수 있다

### 로컬 릴리스 설치본 만들기

1. 프로젝트 루트 `local.properties`에 `RELEASE_STORE_FILE`, `RELEASE_STORE_PASSWORD`, `RELEASE_KEY_ALIAS`, `RELEASE_KEY_PASSWORD`를 넣는다 (#621)
   - 경로는 `/`로 쓴다. 이 파일에서 `\`는 특수문자라 `C:\...`로 쓰면 경로가 틀어진다. 대개 "파일 없음" 경고가 나지만, 경로에 `\u`로 시작하는 폴더(`\upload`, `\users` 등 소문자)가 있으면 `Malformed \uxxxx encoding` 오류가 나고, `local.properties`는 빌드할 때마다 읽으므로 디버그 빌드까지 멈춘다
   - 별칭은 `keytool -list -keystore <키 파일>`의 `PrivateKeyEntry` 줄 맨 앞 값이다
2. `./gradlew :app:assembleRelease` → `app/build/outputs/apk/release/app-release.apk` (이름에 `-unsigned`가 붙으면 서명이 빠진 것. Gradle 출력의 `릴리스 서명 설정이 불완전해 release 서명을 생략합니다. 확인할 항목: …` 경고에 빠진 값이나 `RELEASE_STORE_FILE(파일 없음)`이 나온다. 네 값이 하나도 읽히지 않으면(키 이름 오타 등) 경고 없이 `-unsigned`만 붙는다)
3. 기기에서 앱을 지운 뒤 `adb install app/build/outputs/apk/release/app-release.apk`

- 이 설치본은 Play Integrity를 통과하지 못했다. Play에서 설치한 앱이 아니기 때문이거나, App Check에는 #608 기준으로 Play 앱 서명 키의 SHA-256을 등록하므로 업로드 키로 서명한 이 설치본과 맞지 않기 때문으로 보인다 (어느 쪽인지 미확인). 2026-11-25 확인 때 Logcat에 `Error getting App Check token; using placeholder token instead. ... 403 body: App attestation failed.`가 나왔다. App Check 강제가 꺼져 있어 동작에는 영향이 없었지만, **강제가 켜지면 이 설치본은 서버 요청이 거부될 것**으로 보인다 ([APP_CHECK.md](APP_CHECK.md#콘솔play-console-설정-처음-한-번)). 키 제한을 다시 확인할 일이 있으면 강제 전에 하거나, 실패 원인이 키 제한인지 App Check인지 Logcat으로 나눠 본다

### 확인 결과

앱 제한 적용 후, 같은 실기기(SM-N986N)에서 확인했다. 아래 표는 2026-11-25에 확인했다.

| 빌드 | 설치본 서명 SHA-1 (앞 4바이트) | 로그인 | 읽기 | 쓰기 |
|---|---|---|---|---|
| 디버그 (PC 1대) | `2A:F4:10:84` | 됨 | 됨 | 됨 (좋아요 → 취소) |
| 내부 테스트 트랙 (versionCode 1) | `74:A7:58:34` | 됨 | 됨 | 됨 |
| 로컬 릴리스 | `E4:B0:17:A8` | 됨 | 됨 | 됨 |
| 로컬 릴리스, 처음 쓰는 계정 | `E4:B0:17:A8` | 됨 (닉네임 설정 화면까지 이동) | — | — |

- 세 빌드 모두 앱 Logcat에 `API key`, `blocked`, `PERMISSION_DENIED`, `API_KEY` 문구가 없었다 (내부 테스트·로컬 릴리스는 앱을 지우고 새로 설치한 뒤 확인했고, 디버그 빌드는 새로 설치했는지 기록하지 않았다)
- 나머지 팀원 2명의 디버그 빌드는 팀원이 직접 확인했다 (팀원 보고: 로그아웃 → 로그인, 좋아요 → 취소 모두 정상, 2026-11-26. 각 PC의 debug SHA-1은 2026-11-25에 Firebase 목록과 대조함. 새로 설치한 직후의 Logcat 확인은 받지 않았다)
- **Crashlytics 보고 (2026-11-26, 로컬 릴리스)**: `adb shell am crash com.a32b.plant`로 크래시를 냈다 (보안 폴더(user 150) 권한 오류가 출력되지만 user 0의 앱은 종료됐다. `am_crash` 로그로 확인). 몇 초 뒤 Crashlytics 전송 작업이 별도 프로세스로 실행되어 Logcat에 `Making request to: https://crashlyticsreports-pa.googleapis.com/v1/firelog/legacy/batchlog` → `Status Code: 200`이 나왔다. 그 프로세스를 `am crash <pid>`로 한 번 더 종료해 크래시는 모두 2번이다. Crashlytics 보고 전송은 앱의 API 키가 아니라 SDK에 들어 있는 별도 키를 쓴다 (firebase-crashlytics 20.0.4 소스 `DataTransportCrashlyticsReportSender`의 `CRASHLYTICS_API_KEY`). 그래서 이 결과는 키 제한의 근거가 아니라, 키 제한 뒤에도 Crashlytics가 동작한다는 확인이다. Crashlytics 콘솔 문제 목록에도 `ActivityThread.throwRemoteServiceException` (`CrashedByAdbException - shell-induced crash`, 버전 1.0.0, 이벤트 2, 사용자 1)로 들어왔다 (2026-11-26 확인)
- **반대 확인 (2026-11-26)**: 7개 목록에 없는 임시 키(`DE:4D:85:CB…`)로 서명한 디버그 빌드를 설치했다
  - 앱을 켜자마자 Firebase Installations 요청이 `403 PERMISSION_DENIED` (`reason: ACCESS_DENIED`, `Permission denied on the service.`)를 받았다. 등록된 세 빌드(디버그는 새로 설치했는지 기록 없음, 위 참고)에서는 나오지 않은 오류라, 키의 애플리케이션 제한이 실제로 걸려 있다고 본다
  - Google 로그인은 계정을 고른 뒤 실패했다. 다만 원인은 키 제한이 아니라 Firebase 로그인 요청 전 단계인 OAuth다 (`Auth.Api.Credentials`: `[status=UNREGISTERED_ON_API_CONSOLE]`, `Auth`: "This android application is not registered to use OAuth2.0"). 이 SHA-1의 Android OAuth 클라이언트가 없기 때문이다 (이 SHA-1은 Firebase에도 등록하지 않았다. Firebase 등록과 클라이언트 생성의 관계는 미확인)
  - 확인 후 임시 키는 지웠고, 기기는 원래 디버그 빌드로 되돌렸다
- 확인하지 않은 기능: Analytics, Callable 함수(출석·보너스박스·탈퇴)
- 확인하지 않은 경우: SHA-1을 추가한 뒤 실패가 풀리는지, Firebase에는 있고 키 제한에만 없는 SHA-1의 증상 (반대 확인은 둘 다 없는 경우만 봤다)

## API 제한

**Firebase가 만든 25개 API 제한을 그대로 둔다.**

- 앱 제한을 걸기 전부터 Android 키에는 아래 25개 API로 제한이 걸려 있었다. Firebase 문서는 Firebase가 자동으로 만든 키를 Firebase 관련 API로 제한한다고 적는다 (<https://firebase.google.com/docs/projects/api-keys>)

  Cloud Datastore API, Cloud Firestore API, Cloud Logging API, Cloud SQL Admin API, Cloud Storage for Firebase API, FCM Registration API, Firebase AI Logic API, Firebase App Check API, Firebase App Distribution API, Firebase App Hosting API, Firebase App Testers API, Firebase Hosting API, Firebase In-App Messaging API, Firebase Installations API, Firebase Management API, Firebase ML API, Firebase Phone Number Verification API, Firebase Realtime Database Management API, Firebase Remote Config API, Firebase Remote Config Realtime API, Firebase Rules API, Firebase SQL Connect API, Identity Toolkit API, ML Kit API, Token Service API

- 이 상태로 등록된 세 빌드(디버그는 새로 설치했는지 기록 없음)의 Logcat에 이 키를 쓰는 Firebase Installations의 `PERMISSION_DENIED`가 없었고(요청이 나간 것 자체는 로그로 확인하지 않음) 로그인도 됐다 (위 "확인 결과"). Firestore와 Crashlytics 전송은 이 키를 쓰지 않으므로(SDK 소스 기준) API 제한의 근거로 보지 않는다. Analytics와 Callable 함수는 확인하지 않았다 (미확인)
- 목록에는 이 앱이 쓰지 않는 API(예: Cloud SQL Admin, Firebase AI Logic, Phone Number Verification)도 있다. 줄이지 않은 이유:
  - 앱 제한은 우회가 쉽지만(Cloud 문서 기준. 같은 문서가 앱 제한과 API 제한을 함께 걸라고 권한다), 기존 API 제한(25개, Firebase가 건 것으로 보임)이 이미 있고, 이 키로 할 수 있는 일은 보안 규칙이 다시 막고 App Check 강제(#608)를 켜면 그것도 막으므로, 목록을 줄여서 얻는 방어가 작다고 판단했다
  - Firebase 문서는 Firebase용 키에서 **Firebase에 필요한 API를 빼지 않도록 조심하라**고 한다. 지표만 보고 줄이면 드물게 쓰는 기능이 조용히 깨질 수 있다
- 나중에 줄이려면: Cloud 콘솔 → **API 및 서비스 → 사용 설정된 API 및 서비스**에서 며칠 동안 API별 요청을 보고, 요청이 있는 API와 Firebase 문서의 "Firebase에 필요한 API" 표를 대조한 뒤 줄인다. 줄인 뒤 "확인할 것"을 다시 하고, 문제가 생기면 **API 제한부터 되돌린다**

## OAuth 동의 화면 (Google 인증 플랫폼)

Cloud 콘솔 → API 및 서비스 → **OAuth 동의 화면**을 누르면 Google 인증 플랫폼(개요, 브랜딩, 대상, 클라이언트, 데이터 액세스, 인증 센터, 설정)으로 이동한다.

| 항목 | 위치 | 값 (2026-11-25, OAuth 클라이언트 행은 2026-11-26) |
|---|---|---|
| 게시 상태 | 대상 | **프로덕션 단계** |
| 사용자 유형 | 대상 | 외부 |
| 앱 이름 | 브랜딩 | `Plant` |
| 사용자 지원 이메일 | 브랜딩 | 입력돼 있음 |
| 로고 | 브랜딩 | **넣지 않음** |
| 애플리케이션 홈페이지 | 브랜딩 | `https://plant-82b09.web.app/` (저장소의 `hosting/public/index.html`이 Firebase Hosting 기본 페이지 그대로다. 운영 사이트의 페이지를 옮겨 온 것이라고 [LEGAL_HOSTING.md](LEGAL_HOSTING.md) 구조 표에 적혀 있다) |
| 개인정보처리방침 링크 | 브랜딩 | `https://plant-82b09.web.app/privacy-policy.html` ([LEGAL_HOSTING.md](LEGAL_HOSTING.md)) |
| 서비스 약관 링크 | 브랜딩 | `https://plant-82b09.web.app/terms.html` |
| 승인된 도메인 | 브랜딩 | `plant-82b09.firebaseapp.com`, `plant-82b09.web.app` |
| 개발자 연락처 이메일 | 브랜딩 | 입력함 |
| 범위 | 데이터 액세스 | 등록된 범위 없음 (민감하지 않은 범위·민감한 범위·제한된 범위 모두 비어 있음) |
| OAuth 클라이언트 | 클라이언트 | Android 7개 + 웹 1개 (2026-11-26 #782에서 13개 → 7개로 정리. 아래 "Android OAuth 클라이언트 정리" 참고) |

- 로고를 넣지 않는 이유: 브랜딩 화면 안내에 따르면 게시 상태가 "테스트 중"이 아닐 때 로고를 올리면 인증을 위해 앱을 제출해야 한다. 로고가 없어도 Google 계정 선택 화면에 "Plant 계정으로 계속"이 표시되고 로그인이 된다 (2026-11-25 확인)
- 브랜딩 화면 오른쪽 "인증 상태"에 "브랜딩을 인증해야 사용자에게 브랜딩이 표시될 수 있습니다"가 떠 있다. 이번 범위에서는 브랜딩 인증을 진행하지 않았다
- "테스트" 상태에서는 등록한 테스트 사용자(최대 100명)만 쓸 수 있고 승인이 7일 뒤 만료된다. 다만 Google 문서는 이름·이메일·프로필 범위만 요청하는 앱은 예외라고 적는다 (<https://support.google.com/cloud/answer/15549945>). 이 앱은 "프로덕션"이라 해당하지 않는다
- 홈페이지·처리방침·약관 링크가 `web.app` 주소라 승인된 도메인에 `plant-82b09.web.app`을 추가했다
- 홈페이지부터 개발자 연락처까지의 값은 2026-11-25에 콘솔에서 채운 값이다. 저장 후 화면은 따로 캡처하지 않았다. 세 링크 주소는 같은 날 HTTP 200으로 열리는 것을 확인했다

## Android OAuth 클라이언트 정리 (#782)

2026-11-26에 Google 인증 플랫폼 → 클라이언트의 Android 클라이언트 13개를 Firebase SHA-1 7개와 대조해, 목록에 없는 6개를 삭제했다.

| 클라이언트 ID (프로젝트 번호 뒤 4글자) | SHA-1 (앞 4바이트) | 생성일 | 마지막 사용일 (콘솔 표시, 2026-11-26 저녁 삭제 직전에 읽은 값) | Firebase 7개 | 처리 |
|---|---|---|---|:---:|---|
| `q4mp` | `C7:8D:96:3A` | 2026-11-21 | 2026-11-25 | 있음 | 남김 |
| `dd9q` | `AC:16:19:01` | 2026-11-21 | 2026-11-26 | 있음 | 남김 |
| `4o4g` | `74:A7:58:34` | 2026-11-18 | 2026-11-26 | 있음 | 남김 |
| `1siu` | `E4:B0:17:A8` | 2026-11-18 | 2026-11-26 | 있음 | 남김 |
| `cj6a` | `BD:54:4F:37` | 2026-06-24 | 2026-11-25 | 있음 | 남김 |
| `aftm` | `2A:F4:10:84` | 2026-03-23 | 2026-11-26 | 있음 | 남김 |
| `jpjk` | `A0:3E:CF:F1` | 2026-03-23 | 2026-11-26 | 있음 | 남김 |
| `u26i` | `23:41:9A:96` | 2026-06-23 | 2026-11-22 | 없음 | 삭제 |
| `cg63` | `03:77:38:C3` | 2026-04-03 | 2026-11-25 | 없음 | 삭제 |
| `59tr` | `E9:06:16:C4` | 2026-03-25 | 2026-11-25 | 없음 | 삭제 |
| `base` | `66:EC:B1:C6` | 2026-03-23 | 2026-11-25 | 없음 | 삭제 |
| `e1qa` | `43:5E:F1:98` | 2026-03-23 | 2026-11-25 | 없음 | 삭제 |
| `uulr` | `11:23:74:AF` | 2026-03-13 | 2026-11-22 | 없음 | 삭제 |
| `59v3` (Web client) | — | 2026-03-13 | 2026-11-26 | — | 대상 아님 |

- 대조 방법: SHA-1은 클라이언트마다 상세 화면을 열어 읽었다. Firebase 기준 목록은 Firebase 콘솔 → 프로젝트 설정 → Android 앱의 SHA-1 7개다 (SHA-256 3개는 제외). 7개와 남긴 7개는 20바이트 전체가 같다
- 남긴 7개가 어떤 키인지:
  - Play Console → 앱 서명 화면(2026-11-26 확인. 지문은 화면에 글자로 나오지 않아 복사 버튼으로 읽음)의 값과 같은 것: 앱 서명 키(사용 중) `AC:16…`, 양자 내성 암호화 키 `C7:8D…`, 이전 앱 서명 키 `74:A7…`, 업로드 키 `E4:B0…`
  - `2A:F4…`는 작성자 PC의 debug SHA-1이다 (위 "확인 결과"의 디버그 PC 1대)
  - 남는 `A0:3E…`, `BD:54…`는 팀원 2명의 debug SHA-1로 본다 (미확인. 팀원에게 값을 받지 않았다)
- 같은 SHA-1로 클라이언트가 2개 이상인 경우는 없었다
- Web client는 ID가 `google-services.json`의 웹 클라이언트 ID(`default_web_client_id`, `SignInScreen.kt`·`AccountManageScreen.kt`에서 읽음)와 같다. 앱 로그인 요청에 쓰는 클라이언트라 건드리지 않았다
- 삭제한 6개의 SHA-1은 이 정리를 문서에 적기 전의 저장소 이력(`git log --all -S`, 콜론 형식과 콜론 없는 소문자 형식 모두)에 없었다. SHA-1이 주로 들어 있는 `google-services.json`은 저장소에 올리지 않는 파일이라, 이 검색으로 알 수 있는 것은 적다. 누구의 키였는지는 확인하지 못했다 (미확인)
- **삭제한 6개도 "마지막 사용일"이 2026-11-22·11-25로 최근이었다.** 이유는 확인하지 못했다 (미확인)
  - 날짜가 클라이언트마다 달라서(삭제한 6개는 11-22·11-25, 남긴 7개는 11-25·11-26), 로그인 한 번에 모든 클라이언트가 함께 찍힌다고 보기는 어렵다. 다만 클라이언트 상세 화면 안내에 이 값이 하루 이상 늦을 수 있다고 나와 있어 확정은 아니다
  - 그 SHA-1로 서명한 빌드가 그 날짜에 로그인했을 가능성도 있지만 확인하지 못했다. 남긴 클라이언트 중 `dd9q`(사용 중인 앱 서명 키)는 11-26, `q4mp`(양자 내성 키)는 11-25로 나오지만, 그 키로 서명된 설치본으로 로그인했다는 기록은 이 문서에 없다. 그래서 "마지막 사용일 = 그 키로 서명한 빌드의 로그인"이라고 보기도 어렵다
- 삭제한 이유: 팀이 쓰는 것으로 확인한 빌드(Play 설치본, 로컬 릴리스, 팀원 3명의 디버그)의 서명 SHA-1은 모두 Firebase 7개에 있다 (위 Play Console 대조, 작성자 PC 디버그. 팀원 debug는 #664 기록에 Firebase 목록과 대조했다고 적혀 있고 값은 이 문서에 없다). 그래서 그 밖의 SHA-1의 클라이언트는 이 빌드들에 필요하지 않다고 판단했다. 팀원이 다른 PC를 쓰는지는 확인하지 않았다. 클라이언트가 없는 SHA-1의 빌드는 Google 로그인이 `UNREGISTERED_ON_API_CONSOLE`로 실패했다 (위 반대 확인 1건 기준. 그 SHA-1은 Firebase·키 제한에도 없었다)
- 삭제는 클라이언트 상세 화면 → 삭제로 했다. 삭제 확인 창에 "삭제 후 30일 동안 삭제된 사용자 인증 정보 페이지에서 사용자 인증 정보를 복원할 수 있습니다."라고 나온다. 6개는 **API 및 서비스 → 사용자 인증 정보 → 삭제된 사용자 인증 정보**(클라이언트 화면의 "삭제된 OAuth 클라이언트 복원")에 삭제일 2026-11-26으로 올라 있다. 2026-12-26 무렵까지 복원할 수 있는 것으로 본다
  - 같은 "삭제된 사용자 인증 정보" 화면에 `Android key`·`Browser key`가 삭제일 `1970. 1. 1.`로 보이지만, 사용자 인증 정보 목록에는 두 키가 그대로 있다 (2026-11-26 확인). 이번에 지운 것이 아니다. 왜 이렇게 보이는지는 미확인
- 클라이언트 상세 화면 기준으로 설정 반영에 5분에서 몇 시간이 걸릴 수 있다

### 정리 후 확인

| 빌드 | 서명 SHA-1 (앞 4바이트) | 결과 |
|---|---|---|
| 디버그 (작성자 PC) | `2A:F4:10:84` (설치본 `apksigner`로 확인) | 삭제 직후(2026-11-26 20:27) SM-N986N에서 로그아웃 → Google로 계속하기 → 로그인 성공, 홈 화면까지 이동. Logcat에 `UNREGISTERED_ON_API_CONSOLE` 없음 |
| 로컬 릴리스 | `E4:B0:17:A8` (APK `apksigner`로 확인) | 삭제 약 1시간 뒤(2026-11-26 21:25) 같은 기기에서 앱을 지우고 새로 설치 → Google로 계속하기 → 로그인 성공, 홈 화면까지 이동 (디버그와 같은 사용자). Logcat에 `UNREGISTERED_ON_API_CONSOLE` 없음 |
| 내부 테스트 트랙 (versionCode 1) | `74:A7:58:34` (이전 앱 서명 키. Play 스토어에서 설치한 APK를 기기에서 꺼내 `apksigner`로 확인, 설치 앱 `com.android.vending`) | 삭제 약 2시간 뒤(2026-11-26 22:17) 같은 기기에서 앱을 지우고 Play 스토어 앱 페이지에서 설치 → Google로 계속하기 → 로그인 성공, 홈 화면까지 이동 (디버그와 같은 사용자). Logcat에 `UNREGISTERED_ON_API_CONSOLE` 없음 |
| 팀원 2명 디버그 | `A0:3E:CF:F1`, `BD:54:4F:37`로 본다 (7개에서 Play Console 키 4개와 작성자 PC 디버그를 뺀 2개. 팀원 보고에 SHA-1 값은 받지 않아 미확인) | 팀원 보고: 두 명 모두 로그인 성공. 삭제 뒤(2026-11-26 22:17 내부 테스트 확인 이후) 작성자가 보낸 확인 요청에 대한 보고로, 위 "확인 결과"의 #664 팀원 보고(#782 삭제 전에 받은 것)와는 별개다. 팀원이 확인한 정확한 시각과 Logcat은 받지 않았다 |

- 디버그 확인은 삭제 직후, 로컬 릴리스는 약 1시간 뒤, 내부 테스트는 약 2시간 뒤, 팀원 확인은 내부 테스트 확인 뒤에 요청해 받은 것이라, 모두 위 반영 시간(5분에서 몇 시간)이 다 지나기 전일 수 있다. 남긴 7개는 건드리지 않았으므로 반영 뒤에도 영향이 없을 것으로 본다 (미확인)
- 나중에 어떤 빌드의 Google 로그인이 `UNREGISTERED_ON_API_CONSOLE`로 실패하면, 그 빌드의 서명 SHA-1을 확인하고(설치본은 `apksigner verify --print-certs`, 디버그 빌드는 `signingReport`) 먼저 계속 쓸 빌드인지 정한다
  - 계속 쓸 빌드: 아래 "새 팀원·새 PC가 생기면" 2~4번(Firebase와 키 제한에 SHA-1 추가, 확인)을 한다. 디버그가 아닌 빌드는 4번의 "디버그 빌드"를 그 설치본으로 바꿔 읽고, Firebase와 키 제한 중 이미 SHA-1이 있는 곳의 단계(2번 또는 3번)는 건너뛴다. 키 제한에 없는 SHA-1이면, 클라이언트만 복원해도 Firebase Installations 요청이 막힌다 (위 "확인할 것" 마지막 행)
  - 그다음 Google 인증 플랫폼 → 클라이언트에 그 SHA-1의 클라이언트가 있는지 본다. 없으면 "삭제된 사용자 인증 정보" 목록을 보고, 그 SHA-1의 클라이언트가 있으면 복원하고(2026-12-26 무렵까지), 목록에 없거나 기한이 지났으면 "클라이언트 만들기"로 새로 만든다. 클라이언트가 아직 없으면 4번 로그인은 실패하는 것이 정상이므로, 복원·생성한 뒤 반영(5분에서 몇 시간)을 기다려 4번 확인을 다시 한다
  - 복원을 먼저 하면 Firebase 등록 때 같은 SHA-1의 클라이언트가 둘이 되거나, 등록·복원 중 하나가 거부될 수 있다 (어느 쪽인지, 등록하면 클라이언트가 생기는지 모두 미확인. 아래 "새 팀원·새 PC가 생기면" 2번)
  - 쓰지 않을 빌드(예전 팀원 PC 등): 로그인 실패가 이번 정리에서 의도한 결과다
- 확인이 끝난 뒤 기기는 디버그 빌드로 되돌렸다
- 로그아웃 직후 Logcat에 Firestore `PERMISSION_DENIED`(화분 목록·보상 로그 구독)가 나왔다. 로그아웃 확인(20:27:04) 1초 안에 나왔고 다시 로그인한 20:27:32 이후에는 없어서, 로그아웃으로 이전 사용자의 구독이 끊기며 나온 것으로 보인다 (미확인. 이번 정리와는 관계없는 Firestore 규칙 응답이다)
- #664에서 대조가 남아 있던 앱 서명 키 2개(사용 중인 키 `AC:16…`, 양자 내성 키 `C7:8D…`, Play Console 기준)에도 클라이언트가 있다 (위 대조표)

## Android OAuth 클라이언트 앱 소유권 확인 (#783)

Google 인증 플랫폼 → 개요 → 프로젝트 진단 → 앱 보안의 **"보안 흐름 사용" 경고**는 남아 있다. 남은 Android 클라이언트 7개 중 소유권을 확인할 수 있는 것은 1개였고, 그 1개도 저장하지 않기로 했다.

### 경고 내용 (2026-11-27 확인)

- "앱이 보안 OAuth 흐름을 사용하도록 구성되지 않았으며 명의 도용에 취약할 수 있습니다". 펼치면 "다음 Android 클라이언트의 앱 소유권이 확인되지 않았습니다"와 함께 #782 정리 후 남은 Android 클라이언트 7개 전부가 나온다 (2026-11-25에는 정리 전 13개 전부였다)
- 펼친 목록의 클라이언트 이름은 잘려 보이지만, 각 링크가 가리키는 클라이언트 ID(뒤 4글자)로 7개를 대조했다
- 같은 프로젝트 진단의 다른 항목("결제 계정 확인", "업데이트된 연락처 정보", "프로젝트 연락처", "OAuth 클라이언트를 활성 상태로 유지합니다.", "기존 운영체제")은 문제가 없다는 문구가 나온다

### 공식 문서에 나온 것

- Google 인증 플랫폼 개요 문서는 "보안 흐름 사용" 점검 항목에 PKCE, state 파라미터와 함께 Android 앱 소유권 확인을 넣고, 목적을 앱 사칭(impersonation) 위험을 줄이는 것으로 적는다 (<https://support.google.com/cloud/answer/15548748>)
- OAuth 클라이언트 관리 문서는 Android 클라이언트를 만들 때 소유권 확인을 "(Optional)" 단계로만 적고 OAuth native-app 문서로 연결한다 (<https://support.google.com/cloud/answer/15549257>). 연결된 native-app 문서(<https://developers.google.com/identity/protocols/oauth2/native-app#verify-app-ownership>)에는 Chrome 확장 프로그램의 확인 절차만 있다 (2026-11-27 확인)
- 경고의 "해결 방법 알아보기"는 OAuth 권장사항 문서(<https://developers.google.com/identity/protocols/oauth2/resources/best-practices>)로 연결되는데, Android 소유권 확인 내용은 없다 (2026-11-27 확인)
- 그래서 Android 소유권 확인이 무엇을 기준으로 판정하는지(Play 배포 여부, 어떤 서명 키인지)는 공식 문서로 확인하지 못했다. 아래 판정 기준은 콘솔 결과를 보고 추정한 것이다

### 클라이언트별 결과 (2026-11-27)

Google 인증 플랫폼 → 클라이언트 → 각 Android 클라이언트 상세 → "앱 소유권 확인(선택사항)"의 **소유권 확인** 버튼을 눌렀다. **저장은 누르지 않았다.**

| 클라이언트 | SHA-1 (앞 4바이트) | 키 (위 "Android OAuth 클라이언트 정리" 기준) | 버튼을 누른 결과 |
|---|---|---|---|
| `dd9q` | `AC:16:19:01` | 앱 서명 키 (Play Console에 "사용 중"으로 표시) | "이 클라이언트는 Google Play 스토어 앱이 아니므로 소유권 확인 대상이 아닙니다." |
| `q4mp` | `C7:8D:96:3A` | 양자 내성 암호화 키 | 위와 같음 |
| `4o4g` | `74:A7:58:34` | 이전 앱 서명 키 | **"앱 소유권이 확인되었습니다."** |
| `1siu` | `E4:B0:17:A8` | 업로드 키 | 위 `dd9q`와 같음 |
| `aftm` | `2A:F4:10:84` | 작성자 PC debug | 위 `dd9q`와 같음 |
| `jpjk` | `A0:3E:CF:F1` | 팀원 debug로 봄 (미확인) | 위 `dd9q`와 같음 |
| `cj6a` | `BD:54:4F:37` | 팀원 debug로 봄 (미확인) | 위 `dd9q`와 같음 |

- 확인된 `74:A7…`는 Play 스토어에서 설치한 내부 테스트 설치본의 서명 키다 (위 "정리 후 확인" 표, 2026-11-26에 설치본을 `apksigner`로 확인). Play Console에 "사용 중"으로 나오는 `AC:16…`은 확인 대상이 아니었다. 그래서 Play가 실제로 배포하는 설치본의 서명 키만 확인 대상이 되는 것으로 보인다 (미확인. 문서에 기준이 없고, 기기마다 어느 키로 받는지도 확인하지 않았다)
- 같은 날 Play 스토어 공개 페이지(`https://play.google.com/store/apps/details?id=com.a32b.plant`)를 `curl`로 요청하면 HTTP 404였다 (지금은 내부 테스트 트랙만 있다). 그런데도 `4o4g`가 확인되었으므로, 공개 출시가 확인 조건은 아닌 것으로 보인다 (미확인)
- 버튼을 누르고 저장하지 않은 채 `dd9q`와 `4o4g` 상세 화면을 새로고침하면, 둘 다 다시 "준비가 되면 앱 소유권을 확인하세요"와 **소유권 확인** 버튼 상태로 돌아갔고 패키지명과 SHA-1은 그대로였다. 나머지 5개는 버튼을 누른 뒤 저장하지 않고 화면을 떠났다 (새로고침 뒤 상태는 따로 보지 않았다). 버튼만 누른 뒤에도 개요 경고에는 `4o4g`를 포함한 7개가 그대로 나왔다
- 클라이언트 설정을 저장하지 않았으므로 세 빌드(디버그, 로컬 릴리스, 내부 테스트)의 로그인은 다시 확인하지 않았다

### `4o4g`를 저장하지 않은 이유

- 나머지 6개는 확인할 수 없어서, `4o4g`를 저장해도 경고는 남을 것으로 본다 (미확인. 저장해 보지 않았다)
- 저장하면 실제 배포 중인 키의 클라이언트를 바꾸게 되므로, 반영 시간(5분에서 몇 시간)을 기다린 뒤 세 빌드 로그인을 다시 확인해야 한다
- 저장하면 확인 상태가 유지되는지, 저장 뒤에 되돌릴 수 있는지는 확인하지 않았다 (미확인)

### 다시 볼 때

- Play가 배포하는 서명 키가 바뀌거나(앱 서명 키 업그레이드 등), 확인을 저장하기로 하면 각 클라이언트에서 **소유권 확인** 버튼을 다시 누른다. "앱 소유권이 확인되었습니다"가 나온 클라이언트만 저장하고, 저장 뒤 반영 시간을 기다려 세 빌드 로그인을 확인한다. 로그인이 실패하면 위 "정리 후 확인"의 복구 순서를 따른다
- 업로드 키와 debug 키 클라이언트는 Play가 배포하는 서명 키가 아니라서 확인 대상이 되지 않는 것으로 보인다 (미확인). 이 클라이언트가 남아 있는 한 경고도 남을 것으로 본다
- 이 경고가 있어도 로그인은 되는 것으로 본다. 경고는 2026-11-25와 11-27에 보였고, 그 사이인 11-26 "정리 후 확인"에서 세 빌드와 팀원 디버그 로그인이 모두 성공했다 (11-26 당일에 경고를 따로 보지는 않았다)

## 따로 처리할 것

- 지금 따로 남은 항목은 없다. "보안 흐름 사용" 경고가 남는 이유와 다시 볼 조건은 위 "Android OAuth 클라이언트 앱 소유권 확인 (#783)"에 적었다

## 새 팀원·새 PC가 생기면

debug SHA-1은 PC마다 다르다. 아래 2·3번 두 곳 모두에 있어야 한다 (둘 다 없으면 실패하는 것은 반대 확인으로 봤고, 추가한 뒤 풀리는지는 확인하지 않았다). 5번은 기기마다 하는 작업으로, App Check 강제가 꺼져 있는 동안은 없어도 동작하지만 미리 해 둔다 ([APP_CHECK.md](APP_CHECK.md#디버그-토큰-등록-팀원마다-기기마다)).
Firebase SHA-1과 Cloud 키는 프로젝트 설정을 바꿀 권한이 있는 사람이 등록한다.

1. `./gradlew :app:signingReport` → `Variant: debug`의 `SHA1` 확인
2. Firebase 콘솔 → 프로젝트 설정 → Android 앱 → SHA 인증서 지문에 추가
   - Firebase에도 키 제한에도 없는 SHA-1로 서명한 빌드는 Google 로그인이 `UNREGISTERED_ON_API_CONSOLE`로 실패했다 (위 반대 확인). Firebase에 SHA-1을 등록하면 Google 인증 플랫폼에 그 SHA-1의 Android 클라이언트가 생기는 것으로 보인다 (클라이언트 이름이 `auto created by Google Service`, 미확인)
   - 그 PC의 디버그 빌드에서 로그인이 `UNREGISTERED_ON_API_CONSOLE`로 실패하면 Google 인증 플랫폼 → 클라이언트에 그 SHA-1의 Android 클라이언트가 있는지 본다. 없으면 위 "정리 후 확인"의 복구 순서대로 복원하거나 "클라이언트 만들기"로 만든다
   - 앱이 로그인에 쓰는 값은 `google-services.json`의 웹 클라이언트 ID(`default_web_client_id`)이고, SHA-1 목록은 앱에 들어가지 않는다. 그래서 새 `google-services.json`을 받아 팀원 PC와 CI 시크릿 `GOOGLE_SERVICES_JSON`을 바꾸는 것은 필수가 아니다 (파일을 콘솔과 맞춰 두고 싶을 때만)
3. Cloud 콘솔 → 사용자 인증 정보 → Android 키 → 애플리케이션 제한사항에 `com.a32b.plant` + 같은 SHA-1 추가 (**빠뜨리면 그 PC 디버그 빌드만 키가 막힌다**)
4. 3번을 저장하고 최대 5분이 지난 뒤(위 "앱 제한 적용 절차" 5번. 2번 Firebase 등록이 반영되는 시간은 미확인), 기기의 앱을 지우고 그 PC의 디버그 빌드를 새로 설치한 뒤 로그인, 게시글 좋아요 → 취소로 확인하고, 설치 직후 Logcat에 Firebase Installations `403 PERMISSION_DENIED`가 없는지 본다 (위 "확인할 것"과 같은 방법. 좋아요는 Firestore 요청이라 키 제한을 빠뜨린 것은 Logcat으로 본다)
5. App Check 디버그 토큰 등록 ([APP_CHECK.md](APP_CHECK.md#디버그-토큰-등록-팀원마다-기기마다)) — 이건 SHA-1이 아니라 기기마다 한다. 앱을 지웠다 다시 설치하면 새 토큰이 나올 수 있으므로 4번 확인 뒤에 한다

같은 PC라도 SHA-1이 바뀌는 경우가 있다. `~/.android/debug.keystore`가 새로 만들어지면(폴더를 지움, 새 OS 계정 등) 위 절차를 다시 한다. Play 앱 서명 키를 업그레이드하거나 업로드 키를 재설정해 새 SHA-1이 생겨도 Firebase와 키 제한에 함께 넣는다. 앱 서명 키를 업그레이드했다면 App Check에 등록한 SHA-256도 바꾼다 (#608 기준으로 App Check에는 앱 서명 키의 SHA-256을 등록한다, [APP_CHECK.md](APP_CHECK.md#콘솔play-console-설정-처음-한-번)).

Play **내부 앱 공유**(Internal app sharing)를 쓰게 되면, Play 문서 기준으로 그 설치본은 업로드 키나 앱 서명 키가 아니라 Google이 만든 내부 앱 공유 키로 다시 서명된다 (<https://support.google.com/googleplay/android-developer/answer/9844679>). 이 키의 SHA-1은 위 7개에 없으므로, 쓰기 전에 Play Console의 내부 앱 공유 화면의 "Internal test certificate"(내부 테스트 인증서) 항목에서 지문을 확인해 Firebase와 키 제한에 넣어야 할 것으로 보인다 (미확인. 이번에는 내부 앱 공유 설치본을 확인하지 않았다).

팀원이 빠지면 Firebase SHA-1, 키 제한 SHA-1, 그 팀원의 App Check 디버그 토큰을 지운다. Google 인증 플랫폼 → 클라이언트에 그 SHA-1의 Android 클라이언트가 남아 있으면 #782와 같은 방법으로 정리한다.

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
