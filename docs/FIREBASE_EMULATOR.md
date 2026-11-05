# Firebase 에뮬레이터 실행 방법

> 실제 Firebase 서버 대신 **내 PC에서** Auth·Firestore·Cloud Functions를 실행해 보는 환경 (#452).
> 에뮬레이터로 켠 Auth·Firestore·Functions의 데이터는 실제 서버와 분리되어 있어, 마음껏 쓰고 지워도 실제 데이터·요금에 영향이 없다.
>
> ⚠️ 단, **함수 코드에서 에뮬레이터로 켜지 않은 서비스**(Storage, Realtime Database 등)를 호출하면 **실서버로 간다.**
> 그런 서비스를 함수에서 쓰게 되면 `firebase.json`의 `emulators`에 해당 에뮬레이터를 먼저 추가할 것. (아래 "실행 시 나오는 경고" 참고)

## 준비물 (처음 한 번)

| 항목 | 버전 | 확인 명령 |
|---|---|---|
| Node.js | 24 (`functions/package.json`의 `engines`), 규칙 테스트는 **24.12 이상** | `node -v` |
| Java (JDK) | 21 이상 (Firestore 에뮬레이터 실행에 필요) | `java -version` |
| Firebase CLI | 최신 (`npm install -g firebase-tools`) | `firebase --version` |

```bash
firebase login
```

```bash
npm --prefix functions ci
```

## 실행

프로젝트 루트(`firebase.json`이 있는 폴더)에서:

```bash
firebase emulators:start
```

- 종료: 터미널에서 `Ctrl + C`
- 처음 실행 시 에뮬레이터 프로그램을 내려받느라 시간이 걸린다

| 서비스 | 주소 | 비고 |
|---|---|---|
| Emulator UI | http://127.0.0.1:4000 | 브라우저로 데이터·사용자·함수 로그 확인 |
| Auth | 127.0.0.1:9099 | |
| Functions | 127.0.0.1:5001 | 함수 코드(`functions/`)를 저장하면 자동으로 다시 불러옴 |
| Firestore | 127.0.0.1:8080 | 규칙은 `firestore.rules`를 사용 (저장하면 자동 반영) |

포트는 `firebase.json`의 `emulators`에서 바꿀 수 있다. "port taken" 오류가 나면 해당 포트를 쓰는 다른 프로그램을 끄거나 포트를 바꾼다.

## 스크립트 실행 후 자동 종료 (테스트용)

```bash
firebase emulators:exec --only auth,firestore,functions "npm --prefix functions test"
```

에뮬레이터를 켜고 → 명령을 실행하고 → 끝나면 에뮬레이터를 끈다. 규칙 테스트·CI에서 사용.

## 보안 규칙 테스트 (`rules-test/`)

`firestore.rules`가 의도대로 허용·거부하는지 자동으로 검사한다 (`@firebase/rules-unit-testing`).
batch + 규칙 검증 방식(#453·#455·#456)에서는 **규칙이 곧 서버 로직**이므로, 규칙을 고치면 반드시 테스트도 함께 추가·수정한다.

처음 한 번:

```bash
npm --prefix rules-test ci
```

실행 (Firestore 에뮬레이터를 켜고 → 테스트 → 자동 종료):

```bash
npm --prefix rules-test test
```

- 프로젝트 ID는 `demo-plant`를 쓴다. `demo-`로 시작하는 ID는 에뮬레이터 전용이라 실제 서버에 접속하지 않는다
- 규칙은 루트의 `firestore.rules`(실제 배포 파일)를 그대로 읽는다
- 에뮬레이터를 이미 켜 둔 상태라면 `npm --prefix rules-test run test:only`로 테스트만 실행할 수 있다
  (이때는 `firebase emulators:start --project demo-plant`로 켜야 같은 프로젝트로 붙는다)

| 파일 | 대상 |
|---|---|
| `test/users.test.js` | `users`, 하위 `pots`·`logs` |
| `test/posts.test.js` | `posts`(작성·수정·좋아요·신고), 하위 `comments`, `reports` |
| `test/others.test.js` | `nicknames`, `feedbacks`, `studying`, `Tags` |

- 테스트 파일은 `test/` 폴더에 `*.test.js`로 만든다 (하위 폴더도 자동 포함)
- ⚠️ `test/` 폴더 안의 `.js` 파일은 이름과 관계없이 **전부 테스트로 실행**된다 (Node 기본 규칙). 공통 함수 파일은 `helpers.js`처럼 `test/` **밖**에 둔다

테스트 이름에 `[현재 허용 - #번호에서 ...]`가 붙은 것은 **알려진 구멍을 현재 동작 그대로 기록한 테스트**다.
해당 이슈에서 규칙을 고치면 `assertSucceeds` → `assertFails`로 바꾼다.

## 데이터를 남기고 싶을 때 (선택)

기본적으로 에뮬레이터를 끄면 데이터가 사라진다. 다음 실행 때 이어서 쓰려면:

```bash
firebase emulators:start --import=./emulator-data --export-on-exit
```

> `emulator-data/`는 개인 테스트 데이터이므로 커밋하지 않는다 (사용 시 `.gitignore`에 추가).

## 실행 시 나오는 경고

| 경고 | 의미 |
|---|---|
| `The following emulators are not running ... will affect production: database, storage, ...` | 켜지 않은 서비스(Realtime DB, Storage 등)를 **함수에서** 호출하면 실제 서버로 간다는 뜻. 현재 함수는 Auth·Firestore만 쓰므로 해당 없음. 새 서비스를 함수에서 쓰게 되면 `firebase.json`에 그 에뮬레이터도 추가할 것 |
| `package.json indicates an outdated version of firebase-functions` | `firebase-functions` 새 버전이 있다는 안내. 동작에는 문제 없음 |

## 참고

- 로그 파일(`firebase-debug.log`, `firestore-debug.log` 등)은 `.gitignore`의 `*.log`로 제외된다.
- 앱(Android)을 에뮬레이터에 연결하는 설정(`useEmulator`)은 아직 넣지 않았다. 필요해지면 디버그 빌드에서만 연결하도록 추가한다 (Android 에뮬레이터에서 PC는 `10.0.2.2`).
