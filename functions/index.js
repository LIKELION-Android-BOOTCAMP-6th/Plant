/**
 * Import function triggers from their respective submodules:
 *
 * const {onCall} = require("firebase-functions/v2/https");
 * const {onDocumentWritten} = require("firebase-functions/v2/firestore");
 *
 * See a full list of supported triggers at https://firebase.google.com/docs/functions
 */

const {setGlobalOptions} = require("firebase-functions");
const {
  onDocumentCreated,
  onDocumentDeleted,
  onDocumentUpdated,
} = require("firebase-functions/firestore");
const {onCall, HttpsError} = require("firebase-functions/https");
const {onSchedule} = require("firebase-functions/scheduler");
// 오류는 logger.error("메시지", error, {uid})처럼 별도 인자로 넘긴다.
// {uid, error}처럼 객체 안에 넣으면 JSON으로 바뀔 때 stack이 빠지고, 일반 Error는
// message까지 빠져 {}만 남는다 (오류 종류마다 남는 값이 다르다).
// 마지막 인자(plain object)는 jsonPayload 필드로 남는다
const logger = require("firebase-functions/logger");
const {randomInt} = require("crypto");
const {initializeApp} = require("firebase-admin/app");
const {getAuth} = require("firebase-admin/auth");
const {
  FieldValue,
  Timestamp,
  getFirestore,
} = require("firebase-admin/firestore");
// 값만 계산하는 함수·표는 테스트를 위해 calculations.js로 분리했다 (#620)
const {
  ATTENDANCE_STATUS,
  computeBonusBoxDeltas,
  computeUnlockedProfileLevel,
  decideAttendance,
  isStaleStudying,
  toKstDateKey,
} = require("./calculations");
// 탈퇴 데이터 삭제는 운영자용 스크립트(scripts/deleteAccountByEmail.js)도 쓰도록 따로 뺐다 (#778)
const {deleteAuthUser, deleteUserData} = require("./accountDeletion");
// 학습 보상 지급·보정은 기준 시각을 넣어 테스트하려고 따로 뺐다 (#609)
const {
  grantStudyReward,
  retryPendingStudyRewards,
} = require("./studyReward");

// For cost control, you can set the maximum number of containers that can be
// running at the same time. This helps mitigate the impact of unexpected
// traffic spikes by instead downgrading performance. This limit is a
// per-function limit. You can override the limit for each function using the
// `maxInstances` option in the function's options, e.g.
// `onRequest({ maxInstances: 5 }, (req, res) => { ... })`.
// NOTE: setGlobalOptions does not apply to functions using the v1 API. V1
// functions should each use functions.runWith({ maxInstances: 10 }) instead.
// In the v1 API, each function can only serve one request per container, so
// this will be the maximum concurrent request count.
//
// region: Firestore DB 위치(asia-northeast3, 서울)와 맞춘다.
// Firestore 트리거는 DB와 같은 지역이어야 하고, 같은 지역이면 지연·비용도 줄어든다.
// 바꾸면 앱 FirebaseModule.provideFunction의 getInstance("...") 지역도 같이 바꿀 것
// (다르면 앱이 함수를 찾지 못해 NOT_FOUND).
// minInstances는 지정하지 않는다(기본 0) → 요청이 없을 때 과금 없음, 콜드 스타트는 감수.
setGlobalOptions({
  region: "asia-northeast3",
  maxInstances: 10,
});

initializeApp();
const db = getFirestore();

// ---------------------------------------------------------------------------
// 댓글 수(commentCount) 집계 (#528)
// ---------------------------------------------------------------------------
// 앱은 commentCount를 쓰지 않고(규칙에서도 거부), 댓글이 생기거나 지워지면
// 이 트리거가 comments 하위 컬렉션의 개수를 세어 저장한다.
// - +1/-1이 아니라 센 값을 저장 → 같은 이벤트로 두 번 실행되거나 재시도돼도 결과가 같다
// - 개수 세기와 저장을 한 트랜잭션에서 한다 → 댓글이 동시에 여러 개 생기거나 지워져
//   트리거가 겹쳐도, 먼저 센 오래된 값이 나중에 덮어쓰지 않는다
// - 게시글이 없으면(deletePost가 댓글과 게시글을 함께 삭제) 아무것도 하지 않고 끝낸다
//   → 오류로 끝나지 않으므로 retry가 켜져 있어도 재시도하지 않는다
// - retry: 일시 오류(네트워크, 트랜잭션 경합 등)로 실패하면 다시 실행해 개수를 맞춘다
//   (센 값을 저장하므로 재시도해도 안전)
const COMMENT_DOCUMENT = "posts/{postId}/comments/{commentId}";
const COMMENT_TRIGGER_OPTIONS = {document: COMMENT_DOCUMENT, retry: true};

/**
 * 게시글의 comments 개수를 세어 commentCount에 저장한다.
 * @param {string} postId 게시글 문서 ID
 * @return {Promise<void>}
 */
async function syncCommentCount(postId) {
  const postRef = db.collection("posts").doc(postId);
  await db.runTransaction(async (transaction) => {
    const postSnapshot = await transaction.get(postRef);
    if (!postSnapshot.exists) {
      logger.info("게시글이 없어 댓글 수 갱신을 건너뜀", {postId});
      return;
    }
    const countSnapshot = await transaction.get(
        postRef.collection("comments").count(),
    );
    const count = countSnapshot.data().count;
    // 이미 같은 값이면 쓰지 않는다 (중복 실행 시 불필요한 쓰기 방지)
    if (postSnapshot.get("commentCount") === count) return;
    transaction.update(postRef, {commentCount: count});
  });
}

exports.syncCommentCountOnCreate = onDocumentCreated(
    COMMENT_TRIGGER_OPTIONS,
    (event) => syncCommentCount(event.params.postId),
);

exports.syncCommentCountOnDelete = onDocumentDeleted(
    COMMENT_TRIGGER_OPTIONS,
    (event) => syncCommentCount(event.params.postId),
);

// ---------------------------------------------------------------------------
// 학습 보상 지급 (#453)
// ---------------------------------------------------------------------------
// 앱은 학습 기록(logs/{sessionId})을 rewardStatus: "pending"으로 저장만 하고(보안 규칙이 검증),
// 보상은 기록이 서버에 생기면 이 트리거가 지급한다.
// - 오프라인에서 저장한 기록은 서버에 도착하는 순간 실행된다 → 쌓인 보상은 온라인 복귀 후 지급
// - 트랜잭션에서 rewardStatus가 "pending"일 때만 지급하고 "granted"/"none"으로 바꾼다
//   → 같은 이벤트로 두 번 실행되거나 재시도돼도 보상은 1번만 지급된다
// - 기록이 없으면(동기화 전에 삭제) 지급하지 않고 끝낸다
// - users/{uid}가 없으면(회원탈퇴로 삭제, #457) 지급하지 않고 끝낸다
//   set(..., {merge: true})는 지워진 users 문서를 다시 만들기 때문에 쓰지 않는다.
//   두 경우 모두 오류로 끝내지 않으므로 재시도하지 않는다
// - retry: 일시 오류(네트워크, 트랜잭션 경합 등)로 실패하면 다시 실행한다
//   (2세대 함수는 실패한 이벤트를 최대 24시간 재시도. 위 pending 확인으로 중복 지급 없음)
//   배포 때 CLI 경고는 "최대 7일"이라고 나오지만 1세대 함수 기준이다.
//   2세대는 24시간: https://firebase.google.com/docs/functions/retries
const STUDY_LOG_DOCUMENT = "users/{uid}/pots/{potId}/logs/{logId}";
const STUDY_REWARD_TRIGGER_OPTIONS = {
  document: STUDY_LOG_DOCUMENT,
  retry: true,
};

// 보상 티어 표(STUDY_REWARD_TIERS)와 computeStudyReward는 calculations.js에,
// 지급 함수(grantStudyReward)는 studyReward.js에 있다

exports.grantStudyReward = onDocumentCreated(
    STUDY_REWARD_TRIGGER_OPTIONS,
    (event) => grantStudyReward(
        db, event.params.uid, event.params.potId, event.params.logId,
    ),
);

// ---------------------------------------------------------------------------
// 지급 실패로 남은 학습 보상(pending) 보정 (#609)
// ---------------------------------------------------------------------------
// 위 트리거가 재시도 기간(24시간)이 지나도 실패해 pending으로 남은 기록을 하루 1번 다시 지급한다.
// - 서버 생성 시각이 25시간보다 오래된 기록만 처리 → 트리거 재시도와 겹치지 않는다.
//   겹쳐 실행돼도 grantStudyReward의 pending 확인으로 1번만 지급된다
// - 정상이라면 매번 0건. 처리 결과를 매번 로그로 남겨 functions:log로 보정 여부를 확인한다
// - 기준 시간·상한·실패 격리는 studyReward.js의 retryPendingStudyRewards에 있다
// - 실패한 기록은 다음 날 실행에서 다시 처리하므로 예약 함수 자체의 재시도는 두지 않는다
const PENDING_REWARD_RETRY_OPTIONS = {
  // 사용자가 적은 새벽에 실행 (KST)
  schedule: "every day 04:00",
  timeZone: "Asia/Seoul",
  // 한 번에 최대 500건 지급 + 5000건 훑기. 기본 60초보다 넉넉하게 둔다
  timeoutSeconds: 300,
};

exports.retryPendingStudyRewards = onSchedule(
    PENDING_REWARD_RETRY_OPTIONS,
    async () => {
      const result = await retryPendingStudyRewards(db, Date.now());
      // 0건이 정상. 0건도 남겨 함수가 실제로 실행됐는지 확인할 수 있게 한다
      logger.info("pending 학습 보상 보정", result);
    },
);

// ---------------------------------------------------------------------------
// 보너스 박스 열기 (#454)
// ---------------------------------------------------------------------------
// 앱이 추첨하고 결과를 쓰던 박스 열기를 서버가 추첨·지급한다 → 뽑기 결과를 조작할 수 없다.
// - 온라인 전용: 오프라인에서는 호출이 실패하고 앱이 네트워크 안내를 보여준다
// - 한 트랜잭션에서 보유량 확인 → 추첨 → 박스 차감·아이템/Gold 지급 → 결과 저장을 한다
// - 멱등: 결과를 users/{uid}/boxOpenings/{requestId}에 저장한다. 같은 requestId로 다시 호출하면
//   (응답을 받지 못해 앱이 재시도) 다시 추첨하지 않고 저장된 결과를 돌려준다
//   boxOpenings는 보안 규칙에 match가 없어 앱이 읽거나 쓸 수 없다 (Admin SDK만 사용)
//   회원탈퇴 시 deleteUserData의 recursiveDelete(users/{uid})가 함께 지운다
// - 박스에서 박스가 나오면 순증감(뽑힌 박스 수 - 연 박스 수)으로 반영한다

// 확률 표 (원본은 서버. 앱 ItemType에는 표시용 이름·가격만 있다)
// type: 앱 ItemType 이름(응답으로 보냄), weight: 1000분율(합 1000), field: users.item 필드 키
// gold: users.gold에 더할 값
const BONUS_BOX_DROP_TABLE = [
  {type: "HEART", weight: 600, field: "heart"},
  {type: "SUN", weight: 130, field: "sun"},
  {type: "WATER", weight: 90, field: "water"},
  {type: "FERTILIZER", weight: 50, field: "fertilizer"},
  {type: "NUTRIENT", weight: 20, field: "nutrient"},
  {type: "BOX", weight: 50, field: "box"},
  {type: "GOLD_300", weight: 35, gold: 300},
  {type: "GOLD_500", weight: 20, gold: 500},
  {type: "GOLD_1000", weight: 5, gold: 1000},
];
const BONUS_BOX_TOTAL_WEIGHT = BONUS_BOX_DROP_TABLE
    .reduce((sum, drop) => sum + drop.weight, 0);

// 한 번에 열 수 있는 최대 개수 (응답·저장 문서 크기 제한용)
// 바꾸면 앱 OpenBonusBoxUseCase.MAX_OPEN_COUNT도 같이 바꿀 것
const BONUS_BOX_MAX_OPEN_COUNT = 100;
// requestId는 문서 ID로 쓰므로 "/" 등을 막는다 (앱은 UUID를 보낸다)
const BONUS_BOX_REQUEST_ID_PATTERN = /^[A-Za-z0-9_-]{1,64}$/;
const BONUS_BOX_NOT_ENOUGH_MESSAGE = "보너스 박스의 개수가 부족합니다.";

/**
 * 확률 표에서 하나를 뽑는다.
 * @return {Object} BONUS_BOX_DROP_TABLE의 항목
 */
function drawBonusBoxOnce() {
  let target = randomInt(BONUS_BOX_TOTAL_WEIGHT);
  for (const drop of BONUS_BOX_DROP_TABLE) {
    if (target < drop.weight) return drop;
    target -= drop.weight;
  }
  // 가중치 합으로 뽑으므로 여기에 오지 않는다
  return BONUS_BOX_DROP_TABLE[0];
}

/**
 * 뽑힌 목록으로 users/{uid} 갱신 값을 만든다. 연 박스 수만큼 item.box를 줄인다.
 * 증감량 계산(순증감 0인 필드 제외 포함)은 calculations.js의 computeBonusBoxDeltas가 한다.
 * @param {Object[]} drops BONUS_BOX_DROP_TABLE의 항목 목록
 * @param {number} openedCount
 * @return {Object<string, FirebaseFirestore.FieldValue>}
 */
function buildBonusBoxUpdates(drops, openedCount) {
  const updates = {};
  Object.entries(computeBonusBoxDeltas(drops, openedCount))
      .forEach(([path, delta]) => {
        updates[path] = FieldValue.increment(delta);
      });
  return updates;
}

/**
 * 박스를 열어 뽑힌 아이템 이름 목록을 돌려준다. 같은 requestId는 한 번만 처리한다.
 * @param {string} uid
 * @param {number} count
 * @param {string} requestId
 * @return {Promise<{items: string[]}>}
 */
async function openBonusBoxes(uid, count, requestId) {
  const userRef = db.collection("users").doc(uid);
  const openingRef = userRef.collection("boxOpenings").doc(requestId);

  return db.runTransaction(async (transaction) => {
    const [openingSnapshot, userSnapshot] = await Promise.all([
      transaction.get(openingRef),
      transaction.get(userRef),
    ]);
    if (openingSnapshot.exists) {
      logger.info("이미 처리된 박스 열기 요청", {uid, requestId});
      return {items: openingSnapshot.get("items") || []};
    }
    if (!userSnapshot.exists) {
      throw new HttpsError("not-found", "사용자 정보를 찾을 수 없습니다.");
    }

    const box = userSnapshot.get("item.box");
    const owned = typeof box === "number" ? box : 0;
    if (owned < count) {
      throw new HttpsError("failed-precondition", BONUS_BOX_NOT_ENOUGH_MESSAGE);
    }

    const drops = Array.from({length: count}, drawBonusBoxOnce);
    const items = drops.map((drop) => drop.type);
    transaction.update(userRef, buildBonusBoxUpdates(drops, count));
    transaction.set(openingRef, {
      count,
      items,
      createdAt: FieldValue.serverTimestamp(),
    });
    return {items};
  });
}

exports.openBonusBoxes = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
  }
  const {count, requestId} = request.data || {};
  if (!Number.isInteger(count) || count <= 0 ||
      count > BONUS_BOX_MAX_OPEN_COUNT) {
    throw new HttpsError(
        "invalid-argument",
        `보너스 박스는 한 번에 1~${BONUS_BOX_MAX_OPEN_COUNT}개까지 열 수 있습니다.`,
    );
  }
  if (typeof requestId !== "string" ||
      !BONUS_BOX_REQUEST_ID_PATTERN.test(requestId)) {
    throw new HttpsError("invalid-argument", "잘못된 요청입니다.");
  }

  try {
    return await openBonusBoxes(request.auth.uid, count, requestId);
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    // 원인은 로그로만 남긴다 (내부 정보 노출 방지)
    logger.error("보너스 박스 열기 실패", error, {uid: request.auth.uid});
    throw new HttpsError("internal", "보너스 박스 열기 실패");
  }
});

// ---------------------------------------------------------------------------
// 출석 체크 (#531)
// ---------------------------------------------------------------------------
// 앱이 기기 날짜로 판단하고 직접 쓰던 출석을 서버가 서버 시각 기준으로 판단·지급한다
// → 기기 시간을 바꾸거나 Firestore API를 직접 호출해도 하루 1번만 출석된다.
// - 온라인 전용: 앱은 오프라인이면 호출하지 않는다
// - 하루 시작은 KST 00:00 고정. 함수는 UTC로 실행되므로 서버 시각에 9시간을 더한 날짜를 쓴다
//   (그대로 쓰면 KST 00:00~09:00 출석이 전날로 판정됨)
// - 계산한 KST 날짜를 dailyCheckThisMonth.lastCheckedDate("yyyy-MM-dd")에 저장한다
//   → 앱 출석판도 이 값으로 "오늘 출석했는지"를 표시해, 서버 판정과 같은 값을 본다
//   "같은 달인가"는 앞 7자리("yyyy-MM") 비교. lastCheckedAt(Timestamp)은 기록용으로 유지
// - 한 트랜잭션에서 확인 → 갱신·지급을 한다 → 연타·재시도·응답 유실 후 재요청에도 1번만 지급
// - 이미 출석했거나 이번 달을 모두 채운 경우는 오류가 아니라 결과(status)로 돌려준다
// - 한 달 출석 상한(MONTHLY_ATTENDANCE_LIMIT), 결과 값(ATTENDANCE_STATUS),
//   날짜 키 계산(toKstDateKey), 출석 판정(decideAttendance)은 calculations.js에 있다

// 출석 보상 표 (원본은 서버). 키: 이번 달 몇 번째 출석인지
// item: users.item의 필드 키, gold: users.gold에 더할 값
// 앱 AttendanceRewardTable은 출석판 표시용 사본 → 바꾸면 함께 바꿀 것
const ATTENDANCE_REWARD_TABLE = {
  2: {item: "heart", amount: 1},
  4: {item: "sun", amount: 1},
  7: {gold: 100},
  9: {item: "water", amount: 1},
  12: {item: "heart", amount: 1},
  14: {gold: 200},
  17: {item: "sun", amount: 1},
  20: {item: "fertilizer", amount: 1},
  21: {gold: 300},
  23: {item: "water", amount: 1},
  25: {item: "nutrient", amount: 1},
  28: {gold: 500},
};

/**
 * 출석을 처리한다. 하루에 여러 번 호출해도 1번만 반영·지급된다.
 * @param {string} uid
 * @param {number} nowMs 서버 시각(ms)
 * @return {Promise<Object>} {status, newCount?, reward?}
 */
async function checkAttendance(uid, nowMs) {
  const userRef = db.collection("users").doc(uid);
  const todayKey = toKstDateKey(nowMs);

  return db.runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    if (!userSnapshot.exists) {
      // reason: 앱이 지역 불일치로 오는 NOT_FOUND와 구분해 세션 만료로 처리한다
      throw new HttpsError(
          "not-found",
          "사용자 정보를 찾을 수 없습니다.",
          {reason: "USER_NOT_FOUND"},
      );
    }

    const decision = decideAttendance(
        userSnapshot.get("dailyCheckThisMonth"), todayKey,
    );
    if (decision.status !== ATTENDANCE_STATUS.SUCCESS) return decision;

    const reward = ATTENDANCE_REWARD_TABLE[decision.newCount] || null;
    const updates = {
      "dailyCheckThisMonth.count": decision.newCount,
      "dailyCheckThisMonth.lastCheckedDate": todayKey,
      "dailyCheckThisMonth.lastCheckedAt": Timestamp.fromMillis(nowMs),
    };
    if (reward && reward.item) {
      updates[`item.${reward.item}`] = FieldValue.increment(reward.amount);
    }
    if (reward && reward.gold) {
      updates.gold = FieldValue.increment(reward.gold);
    }
    transaction.update(userRef, updates);

    return {status: decision.status, newCount: decision.newCount, reward};
  });
}

exports.checkAttendance = onCall(async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
  }

  try {
    return await checkAttendance(request.auth.uid, Date.now());
  } catch (error) {
    if (error instanceof HttpsError) throw error;
    // 원인은 로그로만 남긴다 (내부 정보 노출 방지)
    logger.error("출석 처리 실패", error, {uid: request.auth.uid});
    throw new HttpsError("internal", "출석 처리 실패");
  }
});

// ---------------------------------------------------------------------------
// 화분 삭제 후 하위 학습 기록 정리 (#455)
// ---------------------------------------------------------------------------
// 앱은 화분 문서 삭제 + 유저 학습 시간 차감을 batch 하나로 쓰고(보안 규칙이 검증),
// Firestore는 하위 컬렉션을 함께 지우지 않으므로 남은 logs는 이 트리거가 지운다.
// - 오프라인에서 지운 화분도 서버에 도착하는 순간 실행된다
// - recursiveDelete(Admin SDK)라 보안 규칙의 영향을 받지 않고, batch 500건 제한도 없다
// - 이미 지운 기록은 조회되지 않으므로 같은 이벤트로 두 번 실행되거나 재시도돼도 결과가 같다
// - 회원탈퇴(deleteAccount)가 화분과 logs를 함께 지울 때도 화분마다 실행되는데,
//   이때는 지울 기록이 없어 아무것도 하지 않고 끝난다
// - users/{uid}에는 쓰지 않는다 (대표 화분 lastSelectedPotId는 비우지 않음: 홈에서 다른 화분을 보여준다)
//   → 탈퇴로 지워진 users 문서를 다시 만들 일이 없다
// - retry: 일시 오류로 실패하면 다시 실행해 남은 기록을 지운다 (2세대 함수는 최대 24시간 재시도)
const POT_DOCUMENT = "users/{uid}/pots/{potId}";
const POT_DELETED_TRIGGER_OPTIONS = {document: POT_DOCUMENT, retry: true};

/**
 * 지워진 화분의 하위 logs를 모두 지운다. 여러 번 실행해도 결과가 같다.
 * @param {string} uid
 * @param {string} potId
 * @return {Promise<void>}
 */
async function deletePotStudyLogs(uid, potId) {
  const logs = db.collection("users").doc(uid)
      .collection("pots").doc(potId).collection("logs");
  await db.recursiveDelete(logs);
}

exports.onPotDeleted = onDocumentDeleted(
    POT_DELETED_TRIGGER_OPTIONS,
    (event) => deletePotStudyLogs(event.params.uid, event.params.potId),
);

// ---------------------------------------------------------------------------
// 프로필 이미지 해금 레벨 갱신 (#598)
// ---------------------------------------------------------------------------
// 화분이 레벨업하면 users/{uid}.unlockedProfileLevel을 올려 프로필 이미지 후보(0..값)를 늘린다.
// 레벨업 batch의 보안 규칙(userLeveledUpPot)은 유저 문서에서 item·lastLevelUpPotId만 허용하므로
// 앱이 같은 batch에 이 값을 넣을 수 없다 → Admin SDK로 실행되는 트리거가 갱신한다.
// - potLevel이 올라간 수정만 처리한다. 학습 시간 저장·이름 변경·완료 등은 유저 문서를 읽지 않고 끝낸다
// - 새 값 = min(max(기존 값, 새 potLevel), 최고 레벨) → 값은 올라가기만 하고, 화분을 지워도 해금은 유지된다
//   max 계산이라 같은 이벤트로 두 번 실행되거나 재시도돼도 결과가 같다. 값이 그대로면 쓰지 않는다
// - 오프라인에서 레벨업해도 화분 수정이 서버에 도착하는 순간 실행된다
// - potLevel이 정수가 아니면(비정상 데이터) 로그만 남기고 끝낸다
// - users/{uid}가 없으면(회원탈퇴로 삭제) 쓰지 않고 끝낸다.
//   set(..., {merge: true})는 지워진 users 문서를 다시 만들기 때문에 쓰지 않는다
//   위 두 경우는 오류로 끝내지 않으므로 재시도하지 않는다
// - retry: 일시 오류(네트워크, 트랜잭션 경합 등)로 실패하면 다시 실행한다 (2세대 함수는 최대 24시간 재시도)
const POT_UPDATED_TRIGGER_OPTIONS = {document: POT_DOCUMENT, retry: true};
// 화분 최고 레벨(MAX_POT_LEVEL)과 새 값 계산(computeUnlockedProfileLevel)은
// calculations.js에 있다

/**
 * 레벨업한 화분의 새 레벨로 유저의 unlockedProfileLevel을 올린다. 여러 번 실행해도 결과가 같다.
 * @param {string} uid
 * @param {string} potId
 * @param {*} beforeLevel 수정 전 potLevel
 * @param {*} afterLevel 수정 후 potLevel
 * @return {Promise<void>}
 */
async function unlockProfileLevel(uid, potId, beforeLevel, afterLevel) {
  // potLevel이 바뀌지 않은 수정 (학습 시간 저장, 이름 변경, 완료 등)
  if (beforeLevel === afterLevel) return;
  if (!Number.isInteger(afterLevel)) {
    logger.warn("potLevel이 정수가 아니어서 해금 레벨 갱신을 건너뜀",
        {uid, potId, afterLevel});
    return;
  }
  const previous = Number.isInteger(beforeLevel) ? beforeLevel : 0;
  // 레벨이 내려간 수정은 해금 대상이 아니다
  if (afterLevel <= previous) return;

  const userRef = db.collection("users").doc(uid);
  await db.runTransaction(async (transaction) => {
    const userSnapshot = await transaction.get(userRef);
    if (!userSnapshot.exists) {
      logger.info("유저 문서가 없어 해금 레벨 갱신을 건너뜀", {uid, potId});
      return;
    }
    const next = computeUnlockedProfileLevel(
        userSnapshot.get("unlockedProfileLevel"), afterLevel,
    );
    // 값이 그대로면 쓰지 않는다
    if (next === null) return;
    transaction.update(userRef, {unlockedProfileLevel: next});
  });
}

exports.unlockProfileLevelOnPotLevelUp = onDocumentUpdated(
    POT_UPDATED_TRIGGER_OPTIONS,
    (event) => unlockProfileLevel(
        event.params.uid,
        event.params.potId,
        event.data.before.get("potLevel"),
        event.data.after.get("potLevel"),
    ),
);

// ---------------------------------------------------------------------------
// 회원탈퇴 (#457)
// ---------------------------------------------------------------------------
// 앱이 문서를 하나씩 지우던 탈퇴를 이 함수 하나로 처리한다.
// 지우는 범위·순서·멱등성은 accountDeletion.js에 있다
// (메일 요청을 처리하는 운영자용 스크립트도 같은 함수를 쓴다, #778)
// - 중간에 실패하면 앱이 같은 함수를 다시 호출하고, 남은 단계부터 이어서 처리된다
const DELETE_ACCOUNT_OPTIONS = {
  // 게시글·댓글이 많은 사용자도 끝까지 처리하도록 넉넉하게 둔다
  // 바꾸면 앱의 호출 대기 시간도 함께 바꿀 것
  // (AuthRemoteDataSourceImpl.DELETE_ACCOUNT_TIMEOUT_SECONDS)
  timeoutSeconds: 300,
  memory: "512MiB",
};

// 재인증 후 이 시간(초) 안에 호출해야 한다 → 오래된(탈취된) 토큰으로 탈퇴시키는 것을 막는다
const REAUTH_MAX_AGE_SECONDS = 5 * 60;

exports.deleteAccount = onCall(DELETE_ACCOUNT_OPTIONS, async (request) => {
  if (!request.auth) {
    throw new HttpsError("unauthenticated", "로그인이 필요합니다.");
  }
  const uid = request.auth.uid;

  // auth_time: 마지막으로 로그인·재인증한 시각(초). 앱은 재인증 직후 토큰을 새로 받아 호출한다
  const authTime = request.auth.token.auth_time;
  const nowSeconds = Math.floor(Date.now() / 1000);
  if (typeof authTime !== "number" ||
      nowSeconds - authTime > REAUTH_MAX_AGE_SECONDS) {
    throw new HttpsError(
        "failed-precondition",
        "보안을 위해 Google 계정을 다시 확인한 뒤 시도해주세요.",
    );
  }

  try {
    await deleteUserData(db, uid);
    await deleteAuthUser(getAuth(), uid);
  } catch (error) {
    // 원인은 로그로만 남기고, 앱에는 재시도 안내만 보낸다 (내부 정보 노출 방지)
    logger.error("회원탈퇴 처리 실패", error, {uid});
    throw new HttpsError("internal", "회원탈퇴 처리 실패");
  }

  logger.info("회원탈퇴 완료", {uid});
  return {deleted: true};
});

// ---------------------------------------------------------------------------
// 오래된 "공부 중" 표시(studying) 정리 (#525)
// ---------------------------------------------------------------------------
// 앱은 공부 중 1분마다 studying/{uid}를 다시 쓰며 updatedAt = 서버 시각을 넣고,
// 일시정지·종료하면 문서를 지운다. 공부 중 앱이 강제 종료되거나 기기가 꺼지면 문서가 남으므로
// 이 예약 함수가 updatedAt이 오래된 문서를 지운다 → 학습 화면 상위 3명에 끝난 사람이 남지 않는다
// - 마지막 갱신 후 최대 약 20분(기준 10분 + 실행 간격 10분)까지 남을 수 있다
//   → 강제 종료 시점부터 세면 마지막 갱신이 최대 1분 전이라 약 21분
// - 기준 시간은 앱 갱신 주기(1분, StudyingViewModel.observeTimer)보다 충분히 길어야 한다
//   (짧으면 네트워크가 잠깐 끊긴 공부 중인 사람도 지워짐 → 다음 갱신 때 다시 생기긴 함)
// - updatedAt 조건 쿼리(<)는 updatedAt이 없는 예전 문서를 찾지 못한다 → 컬렉션 전체(공부 중인
//   사람 수만큼, updatedAt 필드만)를 읽어 코드에서 거른다
// - 읽은 뒤 그 사이 앱이 다시 쓴 문서는 지우지 않는다 (lastUpdateTime 조건부 삭제)
// - 여러 번 실행돼도 결과가 같다(멱등). 실패해도 다음 실행에서 다시 처리하므로 재시도는 두지 않는다
const STUDYING_STALE_MS = 10 * 60 * 1000;
const STUDYING_CLEANUP_SCHEDULE = "every 10 minutes";
// 조건부 삭제에서 "그 사이 다시 쓰였거나 이미 지워짐" — 실패가 아니라 건너뛸 대상
const GRPC_FAILED_PRECONDITION = 9;
const GRPC_NOT_FOUND = 5;
// 오래됨 판정(isStaleStudying)은 calculations.js에 있다

/**
 * updatedAt이 기준 시간보다 오래된 studying 문서를 지운다.
 * @param {number} nowMs 현재 시각(ms)
 * @return {Promise<{deleted: number, skipped: number}>}
 */
async function cleanUpStaleStudying(nowMs) {
  const cutoffMs = nowMs - STUDYING_STALE_MS;
  const snapshot = await db.collection("studying").select("updatedAt").get();
  const staleDocs = snapshot.docs.filter(
      (doc) => isStaleStudying(doc.get("updatedAt"), cutoffMs),
  );
  if (staleDocs.length === 0) return {deleted: 0, skipped: 0};

  const writer = db.bulkWriter();
  let deleted = 0;
  let skipped = 0;
  const results = Promise.allSettled(staleDocs.map((doc) =>
    writer.delete(doc.ref, {lastUpdateTime: doc.updateTime}).then(
        () => {
          deleted++;
        },
        (error) => {
          if (error.code === GRPC_FAILED_PRECONDITION ||
              error.code === GRPC_NOT_FOUND) {
            skipped++;
            return;
          }
          throw error;
        },
    ),
  ));
  await writer.close();
  const failures = (await results).filter((r) => r.status === "rejected");
  if (failures.length > 0) {
    logger.error(
        "오래된 studying 문서 일부 삭제 실패 (다음 실행에서 다시 처리)",
        failures[0].reason,
        {deleted, skipped, failed: failures.length},
    );
    throw failures[0].reason;
  }
  return {deleted, skipped};
}

exports.cleanUpStaleStudying = onSchedule(
    STUDYING_CLEANUP_SCHEDULE,
    async () => {
      const result = await cleanUpStaleStudying(Date.now());
      if (result.deleted > 0 || result.skipped > 0) {
        logger.info("오래된 studying 문서 정리", result);
      }
    },
);
