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
firebase emulators:exec --only firestore,functions --project demo-plant "node 실행할스크립트.js"
```

에뮬레이터를 켜고 → 따옴표 안의 명령을 실행하고 → 끝나면 에뮬레이터를 끈다. 명령이 0이 아닌 코드로 끝나면 `emulators:exec`도 실패로 끝난다.

- `--project demo-plant`를 꼭 붙인다. 빼면 `.firebaserc`의 운영 프로젝트 ID로 실행되어, 에뮬레이터로 켜지 않은 서비스 호출이 실서버로 갈 수 있다
- `--only`에는 스크립트가 쓰는 에뮬레이터만 넣는다 (예: 함수 트리거 확인은 `firestore,functions`)
- 규칙 테스트(`npm --prefix rules-test test`)도 내부적으로 이 명령을 쓴다 (`rules-test/package.json`의 `test` 스크립트)
- 함수 자동 테스트는 `functions/test/`에 있다 (아래 "Cloud Functions 테스트" 참고). `firebase-functions-test`는 `firebase-admin` 14를 지원하지 않아 쓰지 않는다 (#554). 테스트도 이 명령으로 에뮬레이터를 켜고 Admin SDK로 문서를 쓰고 읽는 방식이다

### 함수 확인 스크립트 작성 시 주의

> 계속 남길 확인은 일회성 스크립트 대신 `functions/test/`에 테스트로 추가한다 (아래 "Cloud Functions 테스트")

- ⚠️ 일회성 스크립트는 **`functions/` 밖**(개인 임시 폴더 등)에 둔다
  - `functions/` 안에 두면 배포 전 lint(`eslint .`)가 그 파일도 검사해, 오류가 있으면 배포가 멈춘다
  - `functions/test/` 외의 위치는 `firebase.json`의 배포 제외 목록에 없으므로 함수와 함께 서버에 올라간다
- `functions/` 밖에서는 `require("firebase-admin/app")`가 바로 되지 않는다 (가까운 `node_modules`가 없음). 경로를 직접 적어 `require(".../node_modules/firebase-admin/app")`로 불러와도 패키지의 `exports` 규칙 때문에 실패한다
  → 아래처럼 `createRequire`로 `functions/`에서 불러온다. 인자는 폴더가 아니라 **`functions/` 안의 파일 경로**(`functions/index.js`)를 준다
- 프로젝트 루트에서 실행한다 (스크립트 안의 `functions/index.js` 경로가 현재 폴더 기준)

```js
// 예: 댓글 1개를 만들고 트리거가 commentCount를 1로 맞추는지 확인
const path = require("path");
const {createRequire} = require("module");

const requireFromFunctions = createRequire(path.resolve("functions/index.js"));
const {initializeApp} = requireFromFunctions("firebase-admin/app");
const {getFirestore} = requireFromFunctions("firebase-admin/firestore");

initializeApp({projectId: "demo-plant"});
const db = getFirestore();

(async () => {
  const post = db.collection("posts").doc("check-post");
  await post.set({title: "확인용", commentCount: 0});
  await post.collection("comments").doc("c1").set({content: "a"});

  // 트리거는 바로 반영되지 않으므로 최대 20초 동안 1초마다 확인
  let count;
  for (let i = 0; i < 20; i++) {
    count = (await post.get()).get("commentCount");
    if (count === 1) break;
    await new Promise((resolve) => setTimeout(resolve, 1000));
  }
  console.log("commentCount:", count);
  process.exit(count === 1 ? 0 : 1); // 0이 아니면 emulators:exec도 실패
})();
```

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

| 파일 | 대상 |
|---|---|
| `test/users.test.js` | `users`, 하위 `pots`·`logs` |
| `test/studyLogs.test.js` | 학습 기록 저장 batch 검증(기록 생성 + 화분·유저 학습 시간), 보상 확인 표시, 쌓인 보상 조회 (#453) |
| `test/potCompleteDelete.test.js` | 화분 완료·학습 기록 삭제·화분 삭제 batch 검증 (#455) |
| `test/potLevelUp.test.js` | 화분 레벨업 batch 검증(레벨 +1, 학습 시간 조건, 아이템 차감량). 앱 `LevelUpCondition.kt`를 읽어 레벨마다 경계값을 확인하므로 조건 표가 앱과 규칙에서 다르면 실패 (#456) |
| `test/posts.test.js` | `posts`(작성·수정·좋아요·신고), 하위 `comments`, `reports` |
| `test/others.test.js` | `nicknames`, `feedbacks`, `studying`, `Tags` |

- 테스트 파일은 `test/` 폴더에 `*.test.js`로 만든다 (하위 폴더도 자동 포함)
- ⚠️ `test/` 폴더 안의 `.js` 파일은 이름과 관계없이 **전부 테스트로 실행**된다 (Node 기본 규칙). 공통 함수 파일은 `helpers.js`처럼 `test/` **밖**에 둔다

테스트 이름에 `[현재 허용 - #번호에서 ...]`가 붙은 것은 **알려진 구멍을 현재 동작 그대로 기록한 테스트**다.
해당 이슈에서 규칙을 고치면 `assertSucceeds` → `assertFails`로 바꾼다.

### CI에서도 실행됨 (#539)

GitHub Actions(`.github/workflows/rules-test.yml`)가 같은 명령(`npm --prefix rules-test test`)을 자동으로 실행한다.

| 언제 | 결과 확인 위치 |
|---|---|
| `develop`·`main` 대상 PR (열기·푸시할 때마다) | PR의 **Firestore Rules Test** 검사 |
| `develop`·`main`에 머지된 뒤 | PR 화면이 없으므로 **Actions 탭** 또는 해당 커밋 옆 ✅/❌ |

- 규칙 테스트가 하나라도 실패하면 검사가 실패(❌)로 표시된다
  - `develop`은 앱 빌드(`build`)와 함께 **머지 필수 검사**로 지정되어 있어, ❌면 머지 버튼이 막힌다
  - 브랜치 보호 규칙에 등록된 이름은 job 이름인 `rules-test`. **워크플로의 job 이름을 바꾸면 보호 규칙도 함께 바꿀 것** (안 바꾸면 모든 PR이 "Expected — Waiting for status"로 영원히 대기)
- 머지 후 실행은 **각자는 통과했지만 합치면 깨지는 PR 조합**을 잡고, 캐시를 develop·main에 만들어 다른 PR도 재사용하게 한다 (PR에서 만든 캐시는 그 PR 안에서만 쓰임)
- 앱 빌드(`android-build.yml`)와 **별도 워크플로**라 한쪽이 실패해도 다른 쪽 결과에 영향 없음
- 경로 조건 없이 **모든 PR에서 실행**한다 (나중에 머지 필수 검사로 지정해도 건너뛴 PR이 대기 상태로 막히지 않도록)
- 10분 안에 끝나지 않으면(테스트·에뮬레이터 멈춤) 실패로 처리된다. 평소에는 2분 이내
- CI 환경: JDK 21, Node 24, `firebase-tools@15`. 에뮬레이터 프로그램은 `~/.cache/firebase/emulators`로 캐시
- 실패·시간 초과 시 에뮬레이터 로그(`rules-test/*.log`)가 실행 결과의 Artifacts(`rules-test-logs`)에 올라간다 (실행 결과 페이지 **Summary** 맨 아래)
- CI가 있어도 규칙을 고쳤다면 **푸시 전에 로컬에서 먼저 돌려 보는 것**을 권장 (CI 결과를 기다리는 시간 절약)

## Cloud Functions 테스트 (`functions/test/`)

함수(`functions/index.js`)가 주석에 적힌 약속("1번만 지급", "재시도해도 결과가 같다" 등)을 지키는지 자동으로 검사한다 (#620).
테스트는 두 층으로 나뉜다.

| 종류 | 위치 | 무엇을 | 에뮬레이터 |
|---|---|---|---|
| 계산 로직 유닛 테스트 | `test/unit/*.test.js` | `functions/calculations.js`의 함수(보상 티어, KST 날짜, 출석 판정 등) | 필요 없음 |
| 통합 테스트 | `test/integration/*.test.js` | 실제 문서를 쓰고 트리거·Callable이 남긴 결과 문서 확인 | Firestore·Auth·Functions |

처음 한 번:

```bash
npm --prefix functions ci
```

실행:

```bash
npm --prefix functions test
```

- 에뮬레이터 3개(`firestore,auth,functions`)를 켜고 → `test/*/*.test.js`(`unit/`, `integration/`처럼 `test/` **바로 아래 폴더**의 테스트 파일)를 실행 → 자동 종료 (`functions/package.json`의 `test` 스크립트)
  - `test/` 바로 안의 파일이나 두 단계 아래 폴더의 파일은 실행되지 않는다. 새 테스트는 `unit/`·`integration/`에 둔다
  - `test/helpers/`의 파일은 이름이 `*.test.js`가 아니라서 테스트로 실행되지 않는다
- 에뮬레이터 없이 계산 로직만 빠르게: `npm --prefix functions run test:unit`
- 프로젝트 ID는 `demo-plant` (에뮬레이터 전용, 실서버 접속 없음)
- `test/`는 `firebase.json`의 functions `ignore`에 있어 **배포에 포함되지 않는다**. 단 배포 전 lint(`eslint .`)는 테스트 파일도 검사하므로 lint 규칙을 지켜 작성한다 (CI에서도 lint를 먼저 돌린다)

### 테스트 작성 방법

- 값만 계산하는 로직은 `index.js`가 아니라 `calculations.js`에 두고 `index.js`가 require한다
  - `index.js`는 불러오는 순간 `initializeApp()`과 함수 등록을 하므로 테스트에서 직접 require하지 않는다
- 통합 테스트는 `test/helpers/emulator.js`의 헬퍼를 쓴다

| 헬퍼 | 용도 |
|---|---|
| `db` | 테스트용 Admin SDK Firestore (규칙 무시). 시드 데이터 쓰기·결과 읽기 |
| `uniqueId(prefix)` | 테스트마다 겹치지 않는 uid·문서 ID (앞 테스트의 늦은 트리거가 섞이지 않게) |
| `clearFirestore()` / `clearAuth()` | 에뮬레이터 데이터 전체 삭제 (파일의 `before`·`beforeEach`에서 호출). `clearFirestore(TRIGGER_FREE_DATABASE_ID)`처럼 DB를 지정할 수 있다 |
| `getTriggerFreeDb()` | 트리거가 붙지 않는 테스트용 DB(`TRIGGER_FREE_DATABASE_ID`). 트리거가 바로 처리해 버려 만들 수 없는 상태(지급 실패로 남은 `pending` 기록 등)를 만들 때 쓴다. 대상 함수가 `db`를 인자로 받아야 한다 (예: `studyReward.js`) (#609) |
| `waitForDoc(ref, predicate)` | 트리거 결과가 반영될 때까지 기다림 (기본 20초, 0.2초 간격). 문서가 없으면 `undefined`를 넘긴다 |
| `waitFor(check)` | 문서 하나가 아닌 조건(쿼리 결과 개수 등)을 기다릴 때. `check`가 참인 값을 돌려줄 때까지 기다리며 `0`·`""`도 "아직"으로 보므로 boolean으로 돌려준다 (예: `async () => (await query.get()).size === 2`) |
| `callFunction(name, data, {uid})` | Callable 호출. `uid`를 주면 Auth 에뮬레이터에서 그 uid로 로그인한 토큰으로 호출, 빼면 로그인 안 한 호출. ⚠️ `uid`로 부르면 **호출할 때마다 로그인하고 계정이 없으면 새로 만든다** → 계정 삭제 확인·삭제 뒤 재호출 테스트는 `signIn`으로 받은 토큰을 `{idToken}`으로 재사용 |
| `signIn(uid)` | ID 토큰만 따로 받을 때 (`callFunction(..., {idToken})`에 넘김) |
| `CallableError` | Callable 실패 시 던지는 오류. `code`는 HttpsError 코드 그대로(`"not-found"` 등), `details`도 담김 |

```js
// 트리거: 문서를 쓰고 → 트리거가 남기는 표시가 바뀔 때까지 기다린 뒤 → 결과 확인
await logRef.set({studyingTime: HOUR_MS, rewardStatus: "pending"});
const log = await waitForDoc(logRef, (data) => data && data.rewardStatus !== "pending");
assert.strictEqual(log.rewardStatus, "granted");

// Callable: 성공은 result, 실패는 CallableError
const result = await callFunction("checkAttendance", {}, {uid});
await assert.rejects(callFunction("checkAttendance"), (e) => e.code === "unauthenticated");
```

- ⚠️ 트리거는 문서를 쓴 뒤 따로(비동기로) 실행된다. 쓰고 바로 읽지 말고 `waitForDoc`으로 기다린다
- ⚠️ "아무것도 바뀌지 않음"(두 번째 실행은 지급 안 함 등)은 기다려도 끝을 알 수 없다. 트리거가 끝났다는 표시(`rewardStatus` 등)를 먼저 기다린 뒤 값을 비교한다
- 함수별 첫 실행은 에뮬레이터가 함수를 준비하느라 몇 초 걸린다 (파일별 첫 테스트 측정 예: Windows PC 4~11초, CI 약 2초. 실행마다 달라진다)
- 예약 함수(`cleanUpStaleStudying`, `retryPendingStudyRewards`)는 Pub/Sub 에뮬레이터를 켜지 않아 에뮬레이터에 등록되지 않는다 (실행 로그: `function ignored because the pubsub emulator does not exist or is not running`)
  - 그래서 `retryPendingStudyRewards`는 본체 함수(`studyReward.js`)를 테스트에서 직접 부르고 기준 시각(`nowMs`)을 넣는다
- ⚠️ 함수 에뮬레이터의 트리거 끄기·켜기(허브 `/functions/disableBackgroundTriggers`·`enableBackgroundTriggers`)는 쓰지 않는다. 켤 때 함수 코드를 다시 읽는데, Windows PC에서 그 순간 firebase CLI가 오류 메시지 없이 종료되는 일이 있었다 (2026-11-20, 끄고 켜는 테스트 파일을 4번 실행해 2번). 남은 Firestore 에뮬레이터가 포트 8080을 막아 다음 실행도 실패한다 → 트리거 없이 확인할 때는 `getTriggerFreeDb()`를 쓴다
- ⚠️ 모든 테스트가 `... does not exist, valid functions are:`(404)나 대기 시간 초과로 실패하고, 실행 로그에 `Failed to load function definition ... Timeout after 10000`이 있으면 코드 문제가 아니라 **함수 목록을 불러오는 시간(기본 10초)이 모자란 것**이다
  - PC가 바쁠 때(Gradle 빌드 동시 실행 등) 생긴다. 다른 작업이 끝난 뒤 다시 돌리거나, 제한 시간(초)을 늘려 실행한다: PowerShell `$env:FUNCTIONS_DISCOVERY_TIMEOUT=60; npm --prefix functions test`
  - CI는 워크플로에서 60초로 지정해 두었다
- 테스트 파일은 `--test-concurrency=1`로 한 파일씩 실행한다 (파일마다 `clearFirestore()`로 데이터를 지우므로 동시에 돌면 서로 지움)

### CI에서도 실행됨

GitHub Actions(`.github/workflows/functions-test.yml`, job 이름 `functions-test`)가 `develop`·`main` 대상 PR과 머지 후에 lint → `npm --prefix functions test`를 실행한다.

- 구성은 규칙 테스트 CI와 같다 (JDK 21, Node 24, `firebase-tools@15`, 에뮬레이터 캐시, 경로 조건 없이 항상 실행)
- 10분 안에 끝나지 않으면 실패로 처리된다 (Windows PC에서 `npm --prefix functions test` 전체 약 37초, 1회 측정)
- 실패·시간 초과 시 에뮬레이터 로그(`functions/*.log`)가 Artifacts(`functions-test-logs`)에 올라간다
- 머지 필수 검사로 지정할지는 별도로 정한다 (지정하면 job 이름을 바꿀 때 보호 규칙도 함께 바꿀 것)

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
