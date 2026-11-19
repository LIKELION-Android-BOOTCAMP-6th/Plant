# Firestore 백업·복원 절차

> 운영 Firestore(`plant-82b09`의 `(default)` 데이터베이스)를 실수로 지우거나 잘못 바꿨을 때 되살리는 방법 (#663).
> Firebase 프로젝트가 `plant-82b09` 하나뿐이라 **이 프로젝트가 곧 운영 환경**이다. 아래 명령은 모두 실제 데이터에 적용된다.
>
> ⚠️ 복원은 **기존 DB를 덮어쓰지 않고 새 데이터베이스를 만든다.** 되살린 데이터는 새 DB에서 필요한 컬렉션만 `(default)`로 다시 옮긴다.

## 현재 설정

2026-11-18~19 설정, 아래 "설정 확인" 명령으로 조회한 값이다. 이 문서의 날짜·시각은 CLI 출력과 같은 **UTC 기준**이다 (한국 시간은 +9시간).

| 항목 | 값 | 의미 |
|---|---|---|
| 예약 백업 | 매일 (`DAILY`), 보관 7일 (`604800s`) | 하루 1번 백업이 생기고 7일 지난 백업은 자동 삭제 |
| 백업 일정 ID | `ec797685-86af-442d-83f7-85d68028162f` | 일정 수정·삭제 시 사용 |
| PITR (특정 시점 복구) | 켬 (`POINT_IN_TIME_RECOVERY_ENABLED`), 보관 7일 (`604800s`) | 최근 7일 안의 시점(분 단위)으로 새 DB를 만들 수 있음 |
| PITR 시작 시점 | `2026-11-18T14:00:00Z` (한국 시간 11-18 23:00) | 켠 직후 조회한 값 (켠 시각은 14:59Z). 이 시각 이전으로는 돌아갈 수 없음. 켠 지 7일이 지나면 "지금부터 7일 전"으로 계속 바뀔 것으로 예상한다 (아직 확인 전, 실제 값은 "설정 확인"으로 본다) |
| 위치 | `asia-northeast3` (서울) | 복원한 DB도 같은 위치에 생김 |
| 삭제 보호 | 켬 (`DELETE_PROTECTION_ENABLED`, 2026-11-19 15:05Z) | `(default)` DB를 지우는 명령이 거부됨 (운영 DB에는 실행해 보지 않았고, 삭제 보호가 켜진 복제 DB로 `Cannot delete the database because delete protection state is set to DELETE_PROTECTION_ENABLED` 거부를 확인함). 복원 DB 삭제 단계에서 이름을 잘못 적어도 운영 DB가 지워지지 않게 켬 |

- PITR을 켠 이유: 백업은 하루 1번이라 백업 직전에 생긴 실수는 최대 하루 치 데이터를 잃는다. PITR은 실수 직전 시점으로 돌아갈 수 있다. PITR을 켜기 전에는 최근 1시간(`3600s`)만 가능했다 (`Tags`를 지웠다가 `readTime`으로 복구한 일이 이 1시간 범위 덕분)
- 백업 시각: 공식 문서 기준으로 매일 다른 시각에 만들어지고 시각을 지정할 수 없다. 일정을 만든 뒤 첫 백업이 언제 생기는지도 문서에 없다. 실제 생성 시각은 백업 목록(`firebase firestore:backups:list --project plant-82b09`)에서 확인한다. 첫 백업의 스냅샷 시각은 일정 생성 약 23시간 뒤인 `2026-11-19T14:00:22Z`였다 (`READY`가 된 건 약 7분 뒤)
- 보관 기간 상한: 매일 일정은 최대 7일, 매주 일정은 최대 14주 (DB당 매일 1개 + 매주 1개까지). 7일보다 오래 남기려면 매주 일정을 따로 추가한다

## 비용

[Firestore 가격](https://cloud.google.com/firestore/pricing) 페이지의 Standard 버전, Seoul (asia-northeast3), 매월 기준 단가 (2026-11-18 확인). 가격은 바뀔 수 있으니 설정을 바꿀 때 다시 확인한다.

| 항목 | 단가 (USD) | 무료 할당량 |
|---|---|---|
| PITR 데이터 | $0.192 / GiB·월 | 없음 |
| 데이터 백업 | $0.038 / GiB·월 | 없음 |
| 복원 작업 | $0.256 / GiB (1회) | 없음 |
| 클론 작업 (PITR 시점 복제) | $0.2568 / GiB (1회) | 없음 |
| 저장된 데이터 (원래 내던 요금) | $0.192 / GiB·월 | 1 GiB (`(default)` DB만) |

계산 방법 (같은 페이지 설명 기준):

- PITR: 한 달 평균 DB 크기 × $0.192
- 백업: 백업 하나의 크기 = 백업 시점의 DB 크기. 매일 백업·7일 보관이면 늘 7개가 있으므로 대략 DB 크기 × 7 × $0.038
- 복원·클론: 1회마다 크기 × 단가. 복원한 DB는 이름이 지정된 DB라 무료 할당량이 없고, 남겨 두는 동안 저장 요금이 붙는다

DB 크기별 월 추정 (백업 + PITR만, 원래 저장 요금 제외):

| DB 크기 | PITR | 백업 7개 | 합계 |
|---|---|---|---|
| 0.1 GiB | $0.019 | $0.027 | 약 $0.05 |
| 1 GiB | $0.192 | $0.266 | 약 $0.46 |
| 10 GiB | $1.92 | $2.66 | 약 $4.6 |

- 실제 DB 크기는 Google Cloud 콘솔 Firestore 데이터베이스 화면, 또는 Cloud Monitoring 지표 `firestore.googleapis.com/storage/data_and_index_storage_bytes`에서 확인한다
- 2026-11-18 측정: `(default)` 475,542바이트 (약 0.45 MiB, 위 지표의 최근 값). 이 크기면 백업 + PITR은 월 $0.001도 안 된다
- 결제 → 예산 및 알림에서 월 예산 알림을 걸어 두면 예상보다 늘어날 때 메일을 받는다

## 준비물

| 항목 | 쓰는 단계 | 비고 |
|---|---|---|
| Firebase CLI (`firebase --version`) | 설정 확인, 백업 목록, 새 DB로 복원, 복원 DB 삭제 | `firebase login` 필요. 프로젝트 소유자 계정 |
| `gcloud` (Google Cloud CLI) | 컬렉션 옮기기 (내보내기·가져오기) | PC에 설치하지 않아도 [Cloud Shell](https://shell.cloud.google.com/?project=plant-82b09)(브라우저)에 들어 있음 |
| Node.js + `firebase-admin` | 문서 수 대조 | Cloud Shell에 Node.js가 들어 있고, Cloud Shell에서는 로그인한 계정 권한으로 Admin SDK가 동작 |

모든 명령에 `--project plant-82b09`를 붙인다. `.firebaserc`의 기본값도 `plant-82b09`이지만, 다른 폴더에서 실행할 때를 대비해 명시한다.

## 설정 확인

```bash
# 백업 일정
firebase firestore:backups:schedules:list --database "(default)" --project plant-82b09

# PITR 켜짐 여부·시작 시점·삭제 보호 (Point In Time Recovery, Earliest Version Time, Delete Protection State)
firebase firestore:databases:get "(default)" --project plant-82b09

# 지금 남아 있는 백업 목록 (이름·스냅샷 시각·상태)
firebase firestore:backups:list --location asia-northeast3 --project plant-82b09
```

- 백업 이름은 `projects/plant-82b09/locations/asia-northeast3/backups/<백업 ID>` 형식이다. 복원할 때 이 전체 이름을 쓴다
- 시각은 UTC(`Z`)로 나온다. 한국 시간은 +9시간

설정을 다시 만들어야 할 때 (일정이 지워졌거나 새 프로젝트):

```bash
firebase firestore:backups:schedules:create --database "(default)" --recurrence DAILY --retention 7d --project plant-82b09
firebase firestore:databases:update "(default)" --point-in-time-recovery ENABLED --delete-protection ENABLED --project plant-82b09
firebase firestore:databases:get "(default)" --project plant-82b09
```

- Firebase CLI는 `--recurrence`에 대문자 `DAILY`/`WEEKLY`만 받는다 (`gcloud`는 소문자 `daily`)
- `databases:update`에는 **PITR과 삭제 보호 옵션을 항상 함께** 적는다. CLI가 바꿀 항목 목록(`updateMask`) 없이 요청을 보내서, 하나만 적었을 때 다른 하나가 꺼지는지는 확인하지 않았다. 실행한 뒤 `databases:get`으로 `Point In Time Recovery`·`Delete Protection State`가 모두 `ENABLED`인지 본다

## 새 DB로 되살리기

복원할 DB 이름(ID)을 정한다. 예: `restore-20261118`. 영문 소문자·숫자·하이픈만 쓴다. 이미 있는 이름은 쓸 수 없다.

### 방법 1. 매일 백업에서 복원

```bash
firebase firestore:databases:restore --database restore-20261118 --backup "projects/plant-82b09/locations/asia-northeast3/backups/<백업 ID>" --project plant-82b09
```

### 방법 2. PITR로 특정 시점 복제 (실수 직전 시각을 알 때)

```bash
firebase firestore:databases:clone "projects/plant-82b09/databases/(default)" projects/plant-82b09/databases/restore-20261118 --snapshot-time 2026-11-20T05:30:00Z --project plant-82b09
```

- 원본·대상 DB를 **전체 경로**(`projects/plant-82b09/databases/...`)로 적는다. `"(default)" restore-20261118`처럼 ID만 적으면 `Error parsing database name` 오류가 난다
- `--snapshot-time`은 UTC로, 분 단위(초는 `00`)로 적는다. 한국 시간 14:30이면 `05:30:00Z`
- `HTTP Error: 503, Deadline expired` 오류가 나면 `firebase firestore:databases:list`로 새 DB가 생기지 않았는지 확인하고 같은 명령을 다시 실행한다 (2026-11-18 테스트 때 첫 시도에서 이 오류가 났고, DB가 생기지 않아 다시 실행해서 성공했다)
- "PITR 시작 시점"보다 이전, 또는 7일보다 오래된 시점은 지정할 수 없다

### 두 방법 공통

- 명령은 복원을 **시작**만 한다. 새 DB는 바로 목록에 보이지만 아직 데이터가 다 들어온 것은 아니다. 아래 명령의 `Done`이 `YES`가 되면 끝난 것이다

  ```bash
  firebase firestore:operations:list --database restore-20261118 --project plant-82b09
  ```
- `Done: YES` 직후 1~2분은 새 DB를 읽으면 `Cannot serve requests when the database is undergoing a restore.` 오류가 날 수 있다. 같은 때 DB 삭제와 설정 변경(`databases:update`)도 `is in the middle of restore`(복제는 `clone`) 오류로 거부된다. 잠시 뒤 다시 시도한다 (2026-11-19 복원·복제 때 겪음)
- 새 DB의 PITR은 꺼진 상태로 만들어진다. **삭제 보호는 복제한 시점의 설정을 이어받을 수 있다** (2026-11-19 확인):

  | 만든 방법 | 원본 시점의 `(default)` 삭제 보호 | 새 DB 삭제 보호 |
  |---|---|---|
  | 매일 백업 복원 (켜기 전 백업) | 꺼짐 | 꺼짐 |
  | PITR 복제 (`15:05:00Z`, 켜기 전) | 꺼짐 | 꺼짐 |
  | PITR 복제 (`15:06:00Z`, 켠 뒤) | 켜짐 | **켜짐** |

  삭제 보호를 켠 뒤에 만든 백업으로 복원했을 때는 아직 확인하지 않았다 (**미확인**). 어느 쪽이든 "복원 DB 삭제" 전에 새 DB의 설정을 확인한다
- 새 DB의 보안 규칙은 **모두 막힌 상태**로 만들어진다. 앱은 `(default)`만 쓰므로 새 DB는 앱에 영향이 없다. 새 DB에 규칙을 배포하지 않는다
- 새 DB에는 Cloud Functions 트리거가 붙지 않는다. 함수는 모두 `(default)` DB를 대상으로 배포되어 있다 (`functions/index.js`의 트리거 옵션에 `database` 지정 없음)

## 문서 수 대조

복원한 DB가 원본과 같은지 컬렉션별 문서 수로 확인한다. Cloud Shell에서:

```bash
mkdir -p ~/plant-restore-check && cd ~/plant-restore-check
npm init -y > /dev/null && npm install firebase-admin
```

`count.js`:

```js
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");

const app = initializeApp({projectId: "plant-82b09"});
// 하위 컬렉션 포함, 경로와 관계없이 같은 ID의 컬렉션을 모두 센다 (collectionGroup)
const COLLECTION_IDS = [
  "users", "pots", "logs", "boxOpenings",
  "posts", "comments", "likes", "activities",
  "studying", "Tags", "nicknames", "reports", "feedbacks",
];

async function countAll(databaseId) {
  const db = getFirestore(app, databaseId);
  const roots = (await db.listCollections()).map((c) => c.id);
  const counts = {};
  for (const id of COLLECTION_IDS) {
    counts[id] = (await db.collectionGroup(id).count().get()).data().count;
  }
  return {roots, counts};
}

const target = process.argv[2];
// 이름을 빠뜨리면 getFirestore가 (default)를 써서 원본끼리 비교하게 된다 → "같음"만 나오는 잘못된 통과를 막는다
if (!target || target === "(default)") {
  console.error("복원 DB 이름을 적는다. 예: node count.js restore-20261118");
  process.exit(1);
}

(async () => {
  const [source, restored] = await Promise.all([countAll("(default)"), countAll(target)]);
  console.log("최상위 컬렉션 (default):", source.roots.join(", "));
  console.log("최상위 컬렉션 복원 DB:", restored.roots.join(", "));
  for (const id of COLLECTION_IDS) {
    const mark = source.counts[id] === restored.counts[id] ? "같음" : "다름";
    console.log(`${id}\t${source.counts[id]}\t${restored.counts[id]}\t${mark}`);
  }
})();
```

```bash
node count.js restore-20261118
```

- 백업·복제 시점 이후 `(default)`에 쌓인 데이터만큼 수가 다를 수 있다. 테스트 사용자가 없는 시간에 하거나, 차이가 그 시간 동안의 활동으로 설명되는지 본다
- 최상위 컬렉션 목록에 `COLLECTION_IDS`에 없는 이름이 보이면 (새 컬렉션이 생긴 것) 목록에 추가하고 이 문서도 고친다
- `count()`는 문서를 다 읽지 않는 집계라 읽기 비용이 적다

## 지운 컬렉션만 다시 옮기기

> ⚠️ 가져오기는 **컬렉션이 통째로 지워졌을 때만** 쓴다. `--collection-ids`는 경로와 관계없이 같은 ID의 컬렉션을 모두 가져오고 같은 ID의 문서를 덮어쓴다. 예를 들어 한 사용자의 화분을 되살리려고 `pots,logs`를 가져오면 **모든 사용자**의 화분·학습 기록이 스냅샷 시점 값으로 돌아가고, 그 뒤에 쌓인 학습 시간·레벨이 사라진다. 일부 문서만 되살릴 때는 아래 "일부 문서만 되살리기"를 따른다

예: `Tags`를 실수로 지웠다. 복원 DB에서 `Tags`만 내보낸 뒤 `(default)`로 가져온다. Cloud Shell에서:

```bash
# 1. 임시 버킷 (DB와 같은 위치)
gcloud storage buckets create gs://plant-82b09-restore-tmp --location=asia-northeast3 --project=plant-82b09

# 2. 복원 DB에서 해당 컬렉션만 내보내기
gcloud firestore export gs://plant-82b09-restore-tmp/tags --database=restore-20261118 --collection-ids=Tags --project=plant-82b09

# 3. (default)로 가져오기
gcloud firestore import gs://plant-82b09-restore-tmp/tags --database="(default)" --collection-ids=Tags --project=plant-82b09

# 4. 임시 버킷 삭제
gcloud storage rm --recursive gs://plant-82b09-restore-tmp
```

- 가져오기는 **같은 문서 ID를 덮어쓰고**, `(default)`에만 있는 문서는 그대로 둔다. 지운 뒤 새로 생긴 문서가 있어도 지워지지 않는다
- `--collection-ids`는 컬렉션 ID 기준이다. 하위 컬렉션까지 옮기려면 ID를 함께 적는다 (예: 모든 사용자의 화분이 통째로 지워졌다면 `--collection-ids=pots,logs`). 특정 컬렉션만 가져오려면 내보낼 때도 `--collection-ids`를 적어야 한다 (전체 내보내기에서 일부만 가져올 수 없음)
- 화분을 지운 경우 `onPotDeleted` 트리거가 그 화분의 `logs`도 이미 지웠다. 그래서 화분을 되살릴 때는 그 화분의 `logs`도 함께 옮긴다
- [공식 문서](https://firebase.google.com/docs/firestore/manage-data/export-import) 기준으로 **가져오기는 Cloud Functions 트리거를 실행하지 않는다.** 그래서:
  - `logs`를 가져와도 `grantStudyReward`가 돌지 않아 보상이 다시 지급되지 않는다
  - 다른 문서에 저장된 집계 값은 자동으로 다시 맞춰지지 않는다 (가져오기를 썼을 때만 해당). 예: 댓글을 지우면 `syncCommentCountOnDelete`가 게시글의 `commentCount`를 이미 줄여 놨는데, 댓글만 가져오면 `commentCount`는 낮은 값으로 남는다 → 가져온 뒤 해당 게시글의 `commentCount`와 실제 댓글 수를 비교해서 맞춘다
- 위 명령은 아직 실제로 실행해 보지 않았다 (**미확인**). 처음 쓸 때 결과를 이 문서에 적는다

### 일부 문서만 되살리기

예: 한 사용자의 화분 하나, 게시글 하나의 댓글 몇 개를 지웠다.

1. Firebase 콘솔 → Firestore에서 데이터베이스를 복원 DB로 바꿔 되살릴 문서의 필드를 본다
2. `(default)`에 같은 경로·같은 문서 ID로 같은 필드를 직접 만든다. 필드 타입(타임스탬프·숫자·문자열)도 원본과 같게 한다. 화분이면 그 화분 아래 `logs` 문서도 함께 만든다
3. 되살린 뒤 앱에서 해당 화면이 정상으로 보이는지 확인한다

- 직접 쓰면 가져오기와 달리 트리거가 실행된다 (`functions/index.js` 기준):
  - `logs` 생성 → `grantStudyReward`: 보상은 `rewardStatus`가 `"pending"`일 때만 지급된다 (`functions/index.js`의 `grantStudyReward`). 이미 처리된 기록(`"granted"`·`"none"`)은 다시 주지 않고 끝난다. 원본이 `"pending"`이었다면 그때 지급된다
  - 댓글 생성 → `syncCommentCountOnCreate`: 실제 댓글 수를 다시 세어 게시글 `commentCount`를 맞춘다
  - 화분 생성에 붙은 트리거는 없다
- 문서가 많으면 콘솔로 옮기기 어렵다. 그때는 복원 DB에서 해당 문서만 읽어 `(default)`에 쓰는 Admin SDK 스크립트가 필요하다 (아직 만들지 않음)
- 이 방법도 아직 실제로 해 보지 않았다 (**미확인**)

## 복원 DB 삭제 (꼭 할 것)

복원 DB는 남겨 두면 저장 비용이 계속 붙는다. 필요한 데이터를 옮기고 대조가 끝나면 지운다.

```bash
# 1. 복원 DB의 삭제 보호 확인 (Delete Protection State)
firebase firestore:databases:get restore-20261118 --project plant-82b09

# 2. ENABLED이면 복원 DB만 끈다 — 이름이 (default)가 아닌지 꼭 본다
firebase firestore:databases:update restore-20261118 --delete-protection DISABLED --point-in-time-recovery DISABLED --project plant-82b09

# 3. 삭제
firebase firestore:databases:delete restore-20261118 --project plant-82b09
```

- ⚠️ DB 이름을 꼭 다시 확인한다. `(default)`는 삭제 보호가 켜져 있어 명령이 거부되지만, 삭제 보호가 꺼져 있으면 **운영 DB가 지워진다**. 지우기 전에 "설정 확인"으로 `(default)`의 `Delete Protection State`가 `ENABLED`인지 본다. `--force`는 붙이지 않고 확인 질문에서 이름을 한 번 더 본다
- 2단계에 `(default)`를 적으면 운영 DB의 삭제 보호와 PITR이 꺼진다. PITR이 꺼지면 켜기 전처럼 최근 1시간 안으로만 돌아갈 수 있다. 2·3단계 모두 DB 이름을 다시 본다
- 성공 메시지에는 DB 이름 대신 내부 ID(`.../databases/9d480eda-...` 같은 UUID)가 나온다. 이름이 안 보여도 정상이다
- 지운 뒤 `firebase firestore:databases:list --project plant-82b09`에 `(default)`만 남았는지 확인한다

## 데이터 초기화 직전 (#642)

비공개 테스트 전 데이터 초기화(`Tags`만 남김)처럼 큰 삭제를 하기 전에:

1. `firebase firestore:backups:list --location asia-northeast3 --project plant-82b09`로 가장 최근 백업이 `READY`인지와 그 스냅샷 시각을 확인한다 (백업 시각이 매일 달라서 간격이 24시간을 넘을 수 있다. 실제 안전장치는 3단계의 PITR이다)
2. 아래 명령으로 `Tags` 문서 수를 세어 적어 둔다 (2026-11-19 기준 32개). Cloud Shell의 `~/plant-restore-check` 폴더("문서 수 대조"에서 `firebase-admin`을 설치한 곳)에서 실행한다

   ```bash
   cd ~/plant-restore-check
   node -e 'const {initializeApp} = require("firebase-admin/app"); const {getFirestore} = require("firebase-admin/firestore"); initializeApp({projectId: "plant-82b09"}); getFirestore().collection("Tags").count().get().then((s) => console.log("Tags", s.data().count));'
   ```
3. 초기화를 시작하기 직전 시각을 UTC로 적어 둔다 (예: `2026-11-20T03:00:00Z`). 문제가 생기면 이 시각으로 "방법 2. PITR로 특정 시점 복제"를 한다
4. 초기화가 끝나면 2단계 명령을 다시 실행해 `Tags` 문서 수가 적어 둔 값과 같은지 확인한다

- `count.js`는 복원 DB와 비교하는 용도라 `(default)`만 세지 못한다 (DB 이름을 빠뜨리거나 `(default)`를 적으면 종료). 그래서 `Tags`는 위 명령으로 센다

## 복원 테스트 기록

| 날짜 | 방법 | 복원 DB | 문서 수 대조 | 삭제 | 비고 |
|---|---|---|---|---|---|
| 2026-11-18 | PITR 복제 (`--snapshot-time 2026-11-18T16:30:00Z`) | `restore-20261118` | 13개 컬렉션 모두 같음 (users 6, pots 8, logs 21, boxOpenings 4, posts 14, comments 25, likes 0, activities 38, studying 0, Tags 32, nicknames 6, reports 2, feedbacks 2) | 삭제함, 목록에 `(default)`만 남음 | 복제 시작부터 `Done: YES`까지 약 10분. 첫 시도는 503 오류로 DB가 생기지 않아 다시 실행함 |
| 2026-11-19 | 매일 백업 복원 (`databases:restore`, 백업 스냅샷 `2026-11-19T14:00:22Z`) | `restore-backup-test` | 13개 컬렉션 모두 같음 (users 7, pots 10, logs 23, boxOpenings 4, posts 14, comments 25, likes 0, activities 38, studying 0, Tags 32, nicknames 7, reports 3, feedbacks 5). 최상위 컬렉션 목록도 같음 | 삭제함, 목록에 `(default)`만 남음 | 일정 생성(`2026-11-18T14:59:15Z`) 후 첫 백업 스냅샷까지 약 23시간. 백업 `CREATING`→`READY` 약 7분, 복원 시작부터 `Done: YES`까지 약 10분. `Done: YES` 직후 1~2분은 "undergoing a restore" 오류로 읽기가 안 됨 → 잠시 뒤 다시 시도 |

- 2026-11-19 삭제 보호를 켠 뒤 임시 DB 3개(`restore-protect-test`, `clone-protect-test`, `clone-protect-test2`)로 "두 방법 공통"의 삭제 보호 표를 확인했다. 삭제 보호가 켜진 `clone-protect-test2`는 바로 지우면 거부되고, "복원 DB 삭제" 1~3단계대로 끈 뒤 지워졌다. 3개 모두 지우고 목록에 `(default)`만 남음 (문서 수 대조는 하지 않음)
- 표의 두 테스트에서 문서 수는 위 `count.js`와 같은 `collectionGroup` 집계를 Firestore REST API(`runAggregationQuery`)로 실행해 셌다 (PC에 `gcloud`·Admin SDK 인증이 없어서 Firebase CLI 로그인 토큰을 씀)
- `count.js` 자체는 Firestore 에뮬레이터에서 두 DB에 다른 개수의 문서를 넣고 실행해 확인했다: 하위 컬렉션(`pots`·`logs`) 포함 개수, 개수가 다를 때 "다름" 표시, 목록에 없는 최상위 컬렉션 표시, DB 이름을 빠뜨리거나 `(default)`를 적으면 안내 문구와 함께 종료 코드 1로 끝남. Cloud Shell에서는 아직 실행하지 않았다 (**미확인**)
- "데이터 초기화 직전" 2단계의 `Tags` 개수 명령은 Firestore 에뮬레이터에서 실행해 개수 출력과 종료 코드 0을 확인했다. Cloud Shell에서는 아직 실행하지 않았다 (**미확인**)
