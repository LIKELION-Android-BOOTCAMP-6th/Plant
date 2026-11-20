// 학습 보상 지급과 지급 실패 보정 (#453, #609)
// index.js의 grantStudyReward 트리거와 retryPendingStudyRewards 예약 함수가 쓰고,
// 테스트가 에뮬레이터 Firestore에 직접 불러 확인한다.
// 기준 시각(nowMs)을 인자로 받아야 "25시간 지난 기록"을 테스트에서 만들 수 있고
// (문서의 서버 생성 시각 createTime은 테스트에서 과거 값으로 만들 수 없다),
// 트리거와 보정이 겹치는 상황도 직접 만들 수 있어 따로 뺐다
// → Firestore 인스턴스(db)는 인자로 받는다 (initializeApp 하지 않음)
const logger = require("firebase-functions/logger");
const {FieldValue} = require("firebase-admin/firestore");
const {computeStudyReward} = require("./calculations");

/**
 * 학습 기록 하나의 보상을 지급한다. 여러 번 실행해도 1번만 지급된다.
 * - 트랜잭션에서 rewardStatus가 "pending"일 때만 지급하고 "granted"/"none"으로 바꾼다
 *   → 트리거 재시도·보정 함수와 겹쳐 실행돼도 보상은 1번만 지급된다
 * - 기록이 없거나(삭제) users/{uid}가 없으면(회원탈퇴) 지급하지 않고 끝낸다 (오류 아님)
 *   set(..., {merge: true})는 지워진 users 문서를 다시 만들기 때문에 쓰지 않는다
 * @param {FirebaseFirestore.Firestore} db
 * @param {string} uid
 * @param {string} potId
 * @param {string} logId
 * @return {Promise<void>}
 */
async function grantStudyReward(db, uid, potId, logId) {
  const userRef = db.collection("users").doc(uid);
  const logRef = userRef.collection("pots").doc(potId)
      .collection("logs").doc(logId);

  await db.runTransaction(async (transaction) => {
    // 트랜잭션은 읽기를 모두 마친 뒤 쓴다
    const [logSnapshot, userSnapshot] = await Promise.all([
      transaction.get(logRef),
      transaction.get(userRef),
    ]);
    if (!logSnapshot.exists) {
      logger.info("학습 기록이 없어 보상 지급을 건너뜀", {uid, potId, logId});
      return;
    }
    // 이미 처리됐거나(granted/none) 이 기능 이전에 만들어진 기록(값 없음)은 지급하지 않는다
    if (logSnapshot.get("rewardStatus") !== "pending") return;
    if (!userSnapshot.exists) {
      logger.info("유저 문서가 없어 보상 지급을 건너뜀", {uid, potId, logId});
      return;
    }

    const reward = computeStudyReward(logSnapshot.get("studyingTime"));
    if (!reward) {
      transaction.update(logRef, {rewardStatus: "none"});
      return;
    }

    const itemUpdates = {"item.box": FieldValue.increment(reward.box)};
    reward.items.forEach((item) => {
      itemUpdates[`item.${item}`] = FieldValue.increment(1);
    });
    transaction.update(userRef, itemUpdates);
    transaction.update(logRef, {rewardStatus: "granted", reward});
  });
}

// ---------------------------------------------------------------------------
// 지급 실패로 남은 pending 기록 보정 (#609)
// ---------------------------------------------------------------------------
// grantStudyReward 트리거는 실패한 이벤트를 최대 24시간만 재시도한다(2세대 함수).
// 그 뒤에도 실패한 기록은 rewardStatus: "pending"으로 남으므로, 예약 함수가 찾아서 다시 지급한다.
// - 기준: 문서의 서버 생성 시각(createTime)이 25시간보다 오래된 기록만 처리한다
//   → 트리거 재시도(24시간)가 아직 진행 중일 수 있는 기록은 건드리지 않는다
//   기록의 createAt은 앱 시각이고, 오프라인에서 만든 기록은 며칠 늦게 서버에 도착할 수 있어 쓰지 않는다
// - 중복 지급 방지는 grantStudyReward의 트랜잭션(pending 확인)에 맡긴다
// - createTime은 조회 조건에 쓸 수 없어(문서 필드가 아님) pending 전체를 페이지로 훑으며 코드에서 거른다
//   정상이라면 pending은 트리거가 처리 중인 몇 건뿐이라 읽기 비용이 작다
// - 한 번에 지급할 개수(maxPerRun)와 훑을 개수(maxScan)에 상한을 둔다 → 장애 직후 pending이
//   아주 많아도 함수 제한 시간 안에 끝나고, 남은 기록은 다음 실행에서 처리한다
// - 기록 하나가 실패해도 나머지는 계속 처리한다 (실패한 기록은 경로와 함께 로그로 남긴다)
// - 같은 유저의 기록은 차례로, 다른 유저끼리는 동시에 지급한다
//   → 지급 트랜잭션은 users/{uid} 문서를 함께 읽고 쓰므로 같은 유저 기록을 동시에 보내면
//     서로 잠금을 기다리고 다시 시도하느라 크게 느려진다
//     (에뮬레이터 측정, 기록 20건씩 동시에 보낼 때: 같은 유저 60건 약 50초, 유저 60명 약 0.5초)
const PENDING_REWARD_MIN_AGE_MS = 25 * 60 * 60 * 1000;
const PENDING_REWARD_PAGE_SIZE = 500;
const PENDING_REWARD_MAX_PER_RUN = 500;
const PENDING_REWARD_MAX_SCAN = 5000;
// 트랜잭션을 한꺼번에 너무 많이 보내지 않도록 동시에 처리하는 유저 수를 이만큼으로 제한한다
// (한 유저의 기록은 차례로 지급하므로 동시에 실행되는 트랜잭션도 최대 이 개수)
const PENDING_REWARD_CONCURRENCY = 20;

/**
 * 학습 기록 경로(users/{uid}/pots/{potId}/logs/{logId})에서 ID를 꺼낸다.
 * collectionGroup("logs")는 이름이 logs인 모든 컬렉션을 조회하므로 모양이 다르면 null을 돌려준다.
 * @param {string} path
 * @return {?{uid: string, potId: string, logId: string}}
 */
function parseStudyLogPath(path) {
  const parts = path.split("/");
  if (parts.length !== 6 || parts[0] !== "users" ||
      parts[2] !== "pots" || parts[4] !== "logs") {
    return null;
  }
  return {uid: parts[1], potId: parts[3], logId: parts[5]};
}

/**
 * 지급 대상을 uid별로 묶는다. 묶음 순서와 묶음 안의 순서는 조회 순서를 따른다.
 * @param {Array<{uid: string}>} targets
 * @return {Map<string, Array<Object>>}
 */
function groupTargetsByUid(targets) {
  const groups = new Map();
  for (const target of targets) {
    if (!groups.has(target.uid)) groups.set(target.uid, []);
    groups.get(target.uid).push(target);
  }
  return groups;
}

/**
 * 서버 생성 시각이 기준보다 오래된 pending 학습 기록의 보상을 다시 지급한다.
 * @param {FirebaseFirestore.Firestore} db
 * @param {number} nowMs 기준 시각(ms). 예약 함수는 Date.now()를 넘긴다
 * @param {Object} [options] 테스트에서 상한·지급 함수를 바꿀 때 쓴다
 * @param {number} [options.pageSize]
 * @param {number} [options.maxPerRun]
 * @param {number} [options.maxScan]
 * @param {function(FirebaseFirestore.Firestore, string, string, string):
 *     Promise<void>} [options.grant]
 * @return {Promise<{scanned: number, skippedRecent: number,
 *     skippedPath: number, retried: number, failed: number,
 *     hasMore: boolean}>}
 */
async function retryPendingStudyRewards(db, nowMs, {
  pageSize = PENDING_REWARD_PAGE_SIZE,
  maxPerRun = PENDING_REWARD_MAX_PER_RUN,
  maxScan = PENDING_REWARD_MAX_SCAN,
  grant = grantStudyReward,
} = {}) {
  const cutoffMs = nowMs - PENDING_REWARD_MIN_AGE_MS;
  const result = {
    scanned: 0, skippedRecent: 0, skippedPath: 0,
    retried: 0, failed: 0, hasMore: false,
  };
  const targets = [];

  // 1. pending 기록을 페이지로 훑어 오래된 것만 모은다 (필드 없이 경로·createTime만 읽음)
  const baseQuery = db.collectionGroup("logs")
      .where("rewardStatus", "==", "pending")
      .select();
  let lastDoc = null;
  for (;;) {
    let query = baseQuery.limit(pageSize);
    if (lastDoc) query = query.startAfter(lastDoc);
    const snapshot = await query.get();
    for (const doc of snapshot.docs) {
      if (targets.length >= maxPerRun || result.scanned >= maxScan) {
        result.hasMore = true;
        break;
      }
      result.scanned++;
      if (doc.createTime.toMillis() > cutoffMs) {
        result.skippedRecent++;
        continue;
      }
      const ids = parseStudyLogPath(doc.ref.path);
      if (!ids) {
        result.skippedPath++;
        continue;
      }
      targets.push({path: doc.ref.path, ...ids});
    }
    if (result.hasMore || snapshot.size < pageSize) break;
    lastDoc = snapshot.docs[snapshot.docs.length - 1];
  }

  // 2. 유저별로 묶어 지급한다. 실패한 기록만 로그로 남기고 계속한다
  //    한 유저의 기록이 실패해도 그 유저의 나머지 기록은 계속 지급한다
  const groups = [...groupTargetsByUid(targets).values()];
  let nextGroup = 0;
  const worker = async () => {
    // 자바스크립트는 한 번에 한 줄씩 실행하므로 nextGroup++가 겹치지 않는다
    while (nextGroup < groups.length) {
      const group = groups[nextGroup++];
      for (const target of group) {
        try {
          await grant(db, target.uid, target.potId, target.logId);
          result.retried++;
        } catch (error) {
          result.failed++;
          // 오류는 별도 인자로 넘겨야 message·stack이 로그에 남는다 (#711)
          logger.error("pending 학습 보상 보정 실패 (다음 실행에서 다시 시도)",
              error, {path: target.path});
        }
      }
    }
  };
  const workerCount = Math.min(PENDING_REWARD_CONCURRENCY, groups.length);
  await Promise.all(Array.from({length: workerCount}, () => worker()));
  return result;
}

module.exports = {
  PENDING_REWARD_MIN_AGE_MS,
  grantStudyReward,
  parseStudyLogPath,
  retryPendingStudyRewards,
};
