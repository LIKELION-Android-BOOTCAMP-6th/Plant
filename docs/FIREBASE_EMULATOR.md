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
- `functions/`에는 테스트 스크립트·테스트 도구가 없다. `firebase-functions-test`가 `firebase-admin` 14를 지원하지 않아 제거함 (#554). 함수 동작은 이 명령으로 에뮬레이터를 켜고 Admin SDK로 문서를 쓰고 읽는 스크립트로 확인한다

### 함수 확인 스크립트 작성 시 주의

- ⚠️ 스크립트는 **`functions/` 밖**(개인 임시 폴더 등)에 둔다
  - `functions/` 안에 두면 배포 전 lint(`eslint .`)가 그 파일도 검사해, 오류가 있으면 배포가 멈춘다
  - `firebase.json`의 배포 제외 목록에 없으므로 함수와 함께 서버에 올라간다
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
