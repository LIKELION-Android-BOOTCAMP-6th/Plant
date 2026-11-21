# Firebase App Check 설정

> 우리 앱이 아닌 곳(스크립트 등)에서 로그인 토큰만으로 Firestore·Callable 함수를 호출하는 것을 막는다 (#608).
> 앱은 요청마다 App Check 토큰을 붙이고, 서버는 강제(enforce)가 켜진 서비스에서 토큰이 없거나 틀린 요청을 거부한다.

## 앱 구성

| 빌드 | 제공자 | 코드 위치 |
|---|---|---|
| release | Play Integrity (`firebase-appcheck-playintegrity`) | `app/src/release/java/com/a32b/plant/AppCheckProvider.kt` |
| debug | 디버그 제공자 (`firebase-appcheck-debug`, `debugImplementation`) | `app/src/debug/java/com/a32b/plant/AppCheckProvider.kt` |

- 설치는 `PlantApplication.onCreate()`의 `installAppCheckProvider()`에서 한다
- 디버그 제공자는 `debugImplementation`이라 release APK에는 들어가지 않는다
- ⚠️ 새 빌드 타입을 추가하면 그 빌드 타입의 소스 폴더(`app/src/<빌드타입>/java/com/a32b/plant/`)에도 `AppCheckProvider.kt`를 만들어야 컴파일된다

## 디버그 토큰 등록 (팀원마다, 기기마다)

디버그 빌드는 Play Integrity를 쓸 수 없어서, 기기에서 만든 **디버그 토큰**을 콘솔에 등록해 "우리 앱"으로 인정받는다.
강제가 꺼져 있는 동안은 등록하지 않아도 앱이 동작하지만, **강제가 켜지면 등록하지 않은 디버그 빌드는 Firestore·함수 요청이 거부된다.**

1. 디버그 빌드를 기기(실기기·에뮬레이터)에 설치하고 실행한다
2. Android Studio Logcat에서 `DebugAppCheckProvider`로 검색한다. 아래 줄의 `:` 뒤 값이 디버그 토큰이다

   ```
   D/com.google.firebase.appcheck.debug.internal.DebugAppCheckProvider: Enter this debug secret into the allow list in the Firebase Console for your project: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
   ```

3. Firebase 콘솔 → **Security → App Check → Apps 탭** → Android 앱 오른쪽 **⋮ → Manage debug tokens** → 토큰 추가 (메뉴 이름은 공식 문서의 영어 표기. 콘솔이 한국어면 이름이 다르게 보일 수 있음)
   - 이름은 누구의 어떤 기기인지 알 수 있게 적는다 (예: `홍길동-갤럭시S23`, `홍길동-에뮬레이터`)
4. 등록 후 앱을 다시 실행하면 요청이 "확인됨"으로 집계된다

- 앱 데이터를 지우거나 앱을 지웠다 다시 설치하면 새 토큰이 나올 수 있다. 강제 후 디버그 빌드에서 갑자기 권한 오류가 나면 logcat의 토큰이 등록한 값과 같은지 먼저 확인한다
- ⚠️ 디버그 토큰은 **비밀번호처럼 다룬다.** 이 토큰과 디버그 빌드가 있으면 확인되지 않은 기기에서도 서버에 접근할 수 있다
  - 저장소·이슈·PR·채팅에 붙여 넣지 않는다
  - 유출되었거나 기기를 더 쓰지 않으면 같은 메뉴(디버그 토큰 관리)에서 바로 삭제한다

## 콘솔·Play Console 설정 (처음 한 번)

| 단계 | 위치 |
|---|---|
| Play Integrity API를 Firebase 프로젝트에 연결 | Play Console → 앱 선택 → Release → **App integrity** → Play Integrity API → Link Cloud project (연결할 프로젝트의 **Owner** 권한 필요. 그룹에 준 Owner 권한은 안 됨) |
| 앱 등록 + 서명 인증서 SHA-256 | Firebase 콘솔 → Security → App Check → Apps 탭 → Android 앱에 Play Integrity를 고르고 SHA-256 입력 |

- 어떤 키의 SHA-256을 넣을지: 이슈 #608 기준으로 **Play 앱 서명 키**. 공식 문서는 "앱 서명 인증서"라고만 적고 앱 서명 키·업로드 키 중 어느 것인지는 적지 않는다. 등록 후 스토어 설치본 요청이 "확인됨"으로 집계되는지 지표로 확인한다

- 토큰 유효 시간(TTL)은 앱 등록 설정에서 정한다 (기본 1시간, 30분~7일). SDK가 만료 전에 알아서 새로 받으므로 매 요청마다 Play Integrity를 부르지 않는다

## 강제(enforce) 진행 순서

1. App Check를 넣은 앱을 비공개 테스트로 배포한다 (강제는 끈 상태)
2. 콘솔 App Check의 서비스별 지표에서 "확인된 요청" 비율을 본다. 확인 안 된 요청이 우리 테스터 기기에서 나오면 원인을 찾는다 (SHA 미등록, 예전 버전 앱, 디버그 토큰 미등록 등)
3. 함수: Callable 3개(`checkAttendance`, `openBonusBoxes`, `deleteAccount`)에 `enforceAppCheck: true`를 넣어 배포한다
4. Firestore: 콘솔 App Check에서 Cloud Firestore 강제를 켠다

문제가 생기면 **강제부터 끄고**(함수는 옵션 제거 후 재배포, Firestore는 콘솔에서 해제) 원인을 확인한다.

## 로컬 에뮬레이터·CI

- 앱은 Firebase 에뮬레이터에 연결하지 않는다 (`useEmulator` 없음, [FIREBASE_EMULATOR.md](FIREBASE_EMULATOR.md) 참고). 앱의 App Check 설치는 에뮬레이터 테스트(`rules-test`, `functions/test`)에 영향이 없다
- PR CI의 `Android Build Check`는 `assembleDebug`·`testDebugUnitTest`만 돌리므로 App Check 토큰이 필요 없다
