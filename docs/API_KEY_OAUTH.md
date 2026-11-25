# Android API 키 앱 제한과 OAuth 동의 화면 (#664)

> `google-services.json`에 들어 있는 Android API 키에 **앱 제한(패키지명 + SHA-1)** 을 걸어, 다른 앱이 이 키를 쓰지 못하게 한다.
> 키 자체는 비밀이 아니다. Firebase 문서도 Firebase용 API 키는 코드나 설정 파일에 넣어도 된다고 한다. 실제 보호는 보안 규칙과 App Check([APP_CHECK.md](APP_CHECK.md))가 맡는다. 앱 제한은 콘솔 설정만으로 거는 추가 방어다.
> Google 로그인 동의 화면(Google 인증 플랫폼)이 팀원이 아닌 계정도 로그인할 수 있는 상태인지도 함께 점검한다.

## 등록해야 하는 SHA-1

API 키 앱 제한에는 **Firebase 프로젝트 설정에 등록된 SHA-1을 모두** 넣는다. 하나라도 빠지면 그 인증서로 서명한 빌드만 키가 막힌다.

| 종류 | 확인 위치 | 비고 |
|---|---|---|
| 팀원 각자의 debug SHA-1 | 각자 PC에서 `./gradlew :app:signingReport` → `Variant: debug`의 `SHA1` | PC(=`~/.android/debug.keystore`)마다 다르다 |
| 업로드 키 SHA-1 | Play Console의 앱 서명 화면 → 업로드 키 인증서 (#621에서 SHA를 가져온 곳) | 로컬 release 빌드(`bundleRelease`)가 이 키로 서명된다 (#621) |
| Play 앱 서명 키 SHA-1 (화면에 나오는 것 전부) | 같은 화면 → 앱 서명 키 인증서 | Play에서 설치한 앱(내부 테스트 트랙 등)이 이 키로 서명된다 (#621). 키를 업그레이드했으면 이전 키와 지금 키가 함께 나오고, 양자 내성 키가 따로 나올 수도 있다 |

- 기준 목록은 Firebase 콘솔 → 프로젝트 설정 → 내 앱 → Android 앱(`com.a32b.plant`)의 "SHA 인증서 지문"이다. API 키 앱 제한의 SHA-1 목록을 이 목록과 1:1로 맞춘다
  - 2026-11-25 기준 SHA-1은 7개다: 팀원 debug 3개, 업로드 키 1개, 앱 서명 키 3개(이전 키, 지금 키, 양자 내성 키)
  - 앱 서명 키가 여러 개인 경우 Play가 기기마다 어느 키로 서명한 앱을 내려주는지는 확인하지 않았다 (미확인). 그래서 화면에 나온 앱 서명 키 SHA-1은 **모두** 넣는다. 빠뜨리면 그 키로 서명된 설치본이 막힐 수 있다
- `google-services.json`의 SHA-1(`oauth_client` 중 `client_type: 1` 항목의 `certificate_hash`)은 기준으로 쓰지 않는다. 2026-11-25에 확인한 로컬 파일에는 5개만 있어 Firebase 콘솔의 7개와 달랐다
- API 키 앱 제한에는 SHA-1을 넣는다. SHA-256은 App Check 등록에 쓴다 ([APP_CHECK.md](APP_CHECK.md#콘솔play-console-설정-처음-한-번))

## 앱 제한 적용 절차

1. Google Cloud 콘솔 → 프로젝트 `plant-82b09` 선택 → **API 및 서비스 → 사용자 인증 정보** → API 키 목록에서 Android 키를 연다
   - Firebase가 만든 키는 이름이 보통 `Android key (auto created by Firebase)`다. 어떤 키인지 헷갈리면 키 값 끝자리를 `google-services.json`의 `api_key.current_key`와 비교한다
2. 지금 제한 상태를 기록한다 (애플리케이션 제한, API 제한 각각 "없음"인지, 무엇이 걸려 있는지)
3. **애플리케이션 제한** → "Android 앱" 선택 → 항목 추가로 패키지 이름 `com.a32b.plant` + SHA-1을 위 표의 SHA-1마다 하나씩 넣는다
   - 항목 하나가 패키지 이름 + SHA-1 한 쌍이다. SHA-1이 여러 개면 패키지 이름이 같아도 항목을 여러 개 만든다 (Cloud 문서 기준. 콘솔 화면 모양은 미확인)
4. **API 제한**은 이 단계에서 바꾸지 않는다 (아래 "API 제한" 참고)
5. 저장한다. 반영 시간은 공식 문서에서 확인하지 못했다 (미확인). 바로 확인했을 때 결과가 예상과 다르면 몇 분 뒤 다시 확인한다
6. 확인 (아래 "확인할 것" 표)

## 확인할 것

| 케이스 | 확인 방법 | 기대 동작 |
|---|---|---|
| 팀원 각자의 디버그 빌드 | 팀원마다 디버그 빌드를 새로 실행 | 로그인·Firestore 읽기·쓰기 정상 |
| 로컬 릴리스 빌드 (업로드 키 서명) | `bundleRelease` 결과물을 기기에 설치해 실행 | 정상 |
| 내부 테스트 트랙 설치본 (Play 앱 서명 키) | 내부 테스트 링크로 설치해 실행 | 정상 |
| 팀원이 아닌 Google 계정 | 위 설치본에서 처음 쓰는 계정으로 로그인 | 동의 화면 통과, 로그인 성공 |
| 팀원 SHA-1 하나가 빠짐 | — | 그 팀원 빌드만 로그인·Firestore 요청 실패. Logcat 오류 문구는 미확인 (#664 이슈는 `API_KEY_ANDROID_APP_BLOCKED`류로 예상). SHA-1을 추가하면 해결 |

- 로컬 release 빌드를 adb로 설치하면 Play Integrity를 통과하지 못하므로, App Check 강제가 켜진 뒤에는 서버 요청이 거부될 것으로 보인다 ([APP_CHECK.md](APP_CHECK.md#콘솔play-console-설정-처음-한-번)). 이 확인은 App Check 강제 전에 하거나, 실패하면 키 제한 문제인지 App Check 문제인지 Logcat으로 나눠 본다

## API 제한

앱 제한을 건 뒤 바로 걸지 않는다. 며칠 동안 지표를 보고 결정한다.

1. Cloud 콘솔 → **API 및 서비스 → 사용 설정된 API 및 서비스**에서 API별 요청 지표를 본다. 이 키에서 온 요청만 골라 볼 수 있는지는 미확인
2. 실제로 요청이 있는 API만 허용 목록 후보로 적는다
3. Firebase 문서가 "Firebase에 필요한 API"로 적은 목록(<https://firebase.google.com/docs/projects/api-keys>) 중 이 앱이 쓰는 SDK와 맞는 것을 빠뜨리지 않는다. 앱이 쓰는 Firebase SDK(`app/build.gradle.kts` 기준): Analytics, Crashlytics, Auth, Firestore, Functions, App Check
   - 이 SDK들이 어떤 API를 부르는지는 지표로 확인한다. 이름만 보고 짐작해 목록을 줄이지 않는다
   - Firebase 문서는 Firebase용 키에 API 제한을 걸 때 **Firebase에 필요한 API를 빼지 않도록 조심하라**고 한다
4. 걸었으면 위 "확인할 것" 표를 다시 확인한다. 특정 기능만 실패하면 **API 제한부터 해제**하고 지표에서 빠진 API를 찾는다
5. 걸지 않기로 했으면 이유를 #664에 남긴다

## OAuth 동의 화면 (Google 인증 플랫폼)

Cloud 콘솔 → **Google 인증 플랫폼**에서 확인한다.

| 항목 | 위치 | 기대 값 |
|---|---|---|
| 사용자 유형 | 대상(Audience) | 외부 |
| 게시 상태 | 대상(Audience) | **프로덕션** |
| 앱 이름 | 브랜딩 | `Plant` |
| 사용자 지원 이메일 | 브랜딩 | 팀이 받는 이메일 |
| 개발자 연락처 | 브랜딩 | 팀이 받는 이메일 |
| 개인정보처리방침 URL | 브랜딩 | #607 사이트의 처리방침 주소 ([LEGAL_HOSTING.md](LEGAL_HOSTING.md)) |
| 로고 | 브랜딩 | **넣지 않음** |
| 사용 범위 | 데이터 액세스 | 기본 범위(`email`, `profile`, `openid`)만 |
| Android OAuth 클라이언트 | 클라이언트 | Firebase에 등록한 SHA-1마다 하나씩 있음 (`google-services.json`에는 5개만 있었으므로 7개 모두 있는지 특히 확인) |

- "테스트" 상태에서는 등록한 테스트 사용자(최대 100명)만 쓸 수 있고 승인이 7일 뒤 만료된다. 다만 Google 문서는 이름·이메일·프로필 범위만 요청하는 앱은 예외라고 적는다. 이 앱은 기본 범위만 쓰므로 "테스트" 상태여도 로그인이 될 수 있지만, 비공개 테스트에서는 혼동을 없애기 위해 "프로덕션"으로 둔다 (출처: <https://support.google.com/cloud/answer/15549945>)
- 로고를 넣으면 앱 이름·로고가 표시되기 전에 확인(심사)을 받아야 할 수 있으므로 넣지 않는다 (#664 기준)
- 브랜딩 정보를 바꾼 뒤 "팀원이 아닌 Google 계정" 로그인을 다시 확인한다

## 새 팀원·새 PC가 생기면

debug SHA-1은 PC마다 다르므로, 아래 세 곳에 모두 추가해야 그 PC의 디버그 빌드가 동작한다.

1. `./gradlew :app:signingReport` → `Variant: debug`의 `SHA1` 확인
2. Firebase 콘솔 → 프로젝트 설정 → Android 앱 → SHA 인증서 지문에 추가 → 새 `google-services.json`을 받아 팀원 PC와 CI 시크릿 `GOOGLE_SERVICES_JSON`을 갱신
3. Cloud 콘솔 → 사용자 인증 정보 → Android 키 → 애플리케이션 제한에 `com.a32b.plant` + 같은 SHA-1 추가 (**빠뜨리면 그 PC 디버그 빌드만 키가 막힌다**)
4. App Check 디버그 토큰 등록 ([APP_CHECK.md](APP_CHECK.md#디버그-토큰-등록-팀원마다-기기마다)) — 이건 SHA-1이 아니라 기기마다 한다

팀원이 빠지면 같은 순서로 Firebase SHA-1, 키 제한 SHA-1을 지운다.

## 되돌리기

- 키 제한은 Cloud 콘솔에서 바로 되돌릴 수 있다. 문제가 생기면 애플리케이션 제한을 **없음**으로 돌리고(API 제한을 걸었다면 그것부터) 원인을 확인한다

## CI·로컬 에뮬레이터 영향

- PR CI의 `Android Build Check`(`.github/workflows/android-build.yml`)는 `assembleDebug`, `testDebugUnitTest`, `assembleDebugAndroidTest`만 돌린다. 앱을 기기에서 실행하지 않으므로 API 키로 요청하지 않는다
- 규칙 테스트(`rules-test`)와 함수 테스트(`functions/test`)는 프로젝트 ID `demo-plant`로 에뮬레이터에만 접속하고 `google-services.json`을 읽지 않는다. 그래서 이 키를 쓰지 않는다
- 앱은 에뮬레이터에 연결하지 않는다 ([FIREBASE_EMULATOR.md](FIREBASE_EMULATOR.md))

## 공식 문서

- API 키 앱 제한: <https://cloud.google.com/docs/authentication/api-keys#adding-application-restrictions>
- Firebase API 키와 필요한 API 목록: <https://firebase.google.com/docs/projects/api-keys>
- OAuth 동의 화면 대상·게시 상태: <https://support.google.com/cloud/answer/15549945>
