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
} = require("firebase-functions/firestore");
const {onCall, HttpsError} = require("firebase-functions/https");
const {onSchedule} = require("firebase-functions/scheduler");
const logger = require("firebase-functions/logger");
const {initializeApp} = require("firebase-admin/app");
const {getAuth} = require("firebase-admin/auth");
const {
  FieldValue,
  Timestamp,
  getFirestore,
} = require("firebase-admin/firestore");

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

const HOUR_MS = 60 * 60 * 1000;

// 보상 티어 표 (원본은 서버. 앱은 "1시간 이상이면 보상" 같은 표시용 기준만 가진다)
// items: users.item의 필드 키, box: 보너스 박스 개수. 시간이 긴 티어부터 확인한다
const STUDY_REWARD_TIERS = [
  {
    hours: 9,
    items: ["heart", "sun", "water", "fertilizer", "nutrient"],
    box: 5,
  },
  {hours: 7, items: ["heart", "sun", "water", "fertilizer"], box: 4},
  {hours: 5, items: ["heart", "sun", "water"], box: 3},
  {hours: 3, items: ["heart", "sun"], box: 2},
  {hours: 1, items: ["heart"], box: 1},
];

/**
 * 학습 시간(ms)에 맞는 보상을 찾는다. 1시간 미만이면 null.
 * @param {*} studyingTime 기록의 studyingTime
 * @return {{items: string[], box: number} | null}
 */
function computeStudyReward(studyingTime) {
  if (typeof studyingTime !== "number" || !Number.isFinite(studyingTime)) {
    return null;
  }
  const hours = Math.floor(studyingTime / HOUR_MS);
  const tier = STUDY_REWARD_TIERS.find((t) => hours >= t.hours);
  return tier ? {items: tier.items, box: tier.box} : null;
}

/**
 * 학습 기록 하나의 보상을 지급한다. 여러 번 실행해도 1번만 지급된다.
 * @param {string} uid
 * @param {string} potId
 * @param {string} logId
 * @return {Promise<void>}
 */
async function grantStudyReward(uid, potId, logId) {
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

exports.grantStudyReward = onDocumentCreated(
    STUDY_REWARD_TRIGGER_OPTIONS,
    (event) => grantStudyReward(
        event.params.uid, event.params.potId, event.params.logId,
    ),
);

// ---------------------------------------------------------------------------
// 회원탈퇴 (#457)
// ---------------------------------------------------------------------------
// 앱이 문서를 하나씩 지우던 탈퇴를 이 함수 하나로 처리한다.
// - 모든 단계는 다시 실행해도 안전하다(멱등). 중간에 실패하면 앱이 같은 함수를
//   다시 호출하고, 이미 지운 문서는 조회되지 않으므로 남은 단계부터 이어서 처리된다
// - 순서: 데이터 → 닉네임 → users/{uid} → Auth 계정
//   Auth 계정이 먼저 사라지면 앱이 다시 호출할 수 없으므로 반드시 마지막에 지운다
// - Admin SDK는 보안 규칙의 영향을 받지 않는다 → #431에서 탈퇴용으로 열어둔 규칙 예외를 제거함
const DELETE_ACCOUNT_OPTIONS = {
  // 게시글·댓글이 많은 사용자도 끝까지 처리하도록 넉넉하게 둔다
  // 바꾸면 앱의 호출 대기 시간도 함께 바꿀 것
  // (AuthRemoteDataSourceImpl.DELETE_ACCOUNT_TIMEOUT_SECONDS)
  timeoutSeconds: 300,
  memory: "512MiB",
};

// 재인증 후 이 시간(초) 안에 호출해야 한다 → 오래된(탈취된) 토큰으로 탈퇴시키는 것을 막는다
const REAUTH_MAX_AGE_SECONDS = 5 * 60;

// 남의 게시글에 남긴 내 댓글은 지우지 않고 익명화한다 (대댓글 흐름·댓글 수 유지)
const DELETED_COMMENT_CONTENT = "- 삭제된 댓글입니다. -";
const ANONYMOUS_COMMENT_USER = {uid: "", nickname: "(알 수 없음)", profileImg: ""};

// 게시글별 트랜잭션을 한꺼번에 너무 많이 보내지 않도록 나눠서 실행한다
const TRANSACTION_CHUNK_SIZE = 20;

/**
 * 문서들을 BulkWriter로 삭제하고, 실패한 문서가 있으면 예외를 던진다.
 * (BulkWriter는 일시 오류를 자동으로 재시도한다)
 * @param {FirebaseFirestore.BulkWriter} writer
 * @param {FirebaseFirestore.DocumentReference[]} refs
 * @return {Promise<void>}
 */
async function deleteRefs(writer, refs) {
  // flush 전에 결과를 모아둬야 실패가 처리되지 않은 Promise로 남지 않는다
  const results = Promise.all(refs.map((ref) => writer.delete(ref)));
  await writer.flush();
  await results;
}

/**
 * items를 size개씩 나눠 순서대로, 각 묶음 안에서는 동시에 실행한다.
 * @template T
 * @param {T[]} items
 * @param {number} size
 * @param {function(T): Promise<void>} task
 * @return {Promise<void>}
 */
async function runInChunks(items, size, task) {
  for (let i = 0; i < items.length; i += size) {
    await Promise.all(items.slice(i, i + size).map(task));
  }
}

/**
 * 게시글의 배열 필드에서 uid를 빼고 카운트를 1 줄인다. 배열에 없으면 아무것도 하지 않는다
 * (재시도 시 카운트가 두 번 줄지 않게 트랜잭션 안에서 다시 확인).
 * @param {FirebaseFirestore.DocumentReference} postRef
 * @param {string} uid
 * @param {string} arrayField likedBy | bookmarkedBy
 * @param {string} countField likeCount | bookmarkCount
 * @return {Promise<void>}
 */
async function removeFromPostArray(postRef, uid, arrayField, countField) {
  await db.runTransaction(async (transaction) => {
    const snapshot = await transaction.get(postRef);
    if (!snapshot.exists) return;
    const values = snapshot.get(arrayField) || [];
    if (!values.includes(uid)) return;
    transaction.update(postRef, {
      [arrayField]: FieldValue.arrayRemove(uid),
      [countField]: FieldValue.increment(-1),
    });
  });
}

/**
 * 탈퇴한 사용자의 Firestore 데이터를 지운다. 여러 번 실행해도 결과가 같다.
 * @param {string} uid
 * @return {Promise<void>}
 */
async function deleteUserData(uid) {
  const writer = db.bulkWriter();
  const userRef = db.collection("users").doc(uid);
  const posts = db.collection("posts");
  const activities = db.collection("activities");

  try {
    // 1. 내 화분 + 하위 logs
    await db.recursiveDelete(userRef.collection("pots"), writer);

    // 2. 내 게시글 + 하위 comments·likes
    //    남이 내 게시글에 남긴 댓글·좋아요·북마크 activity도 대상 게시글이 없어지므로 함께 지운다
    //    (activity를 게시글보다 먼저 지움 → 중간에 실패해도 재시도 때 게시글이 다시 조회되어 이어서 처리)
    const myPosts = await posts.where("author.id", "==", uid).get();
    for (const post of myPosts.docs) {
      const postActivities = await activities
          .where("targetId", "==", post.id).get();
      await deleteRefs(writer, postActivities.docs.map((doc) => doc.ref));
      await db.recursiveDelete(post.ref, writer);
    }

    // 3. 내 activity (게시글·댓글·좋아요·북마크)
    const myActivities = await activities.where("uid", "==", uid).get();
    await deleteRefs(writer, myActivities.docs.map((doc) => doc.ref));

    // 4. 남의 게시글에 남긴 내 댓글 익명화
    //    익명화하면 user.uid가 ""가 되어 재시도 때 다시 조회되지 않는다
    const myComments = await db.collectionGroup("comments")
        .where("user.uid", "==", uid).get();
    const anonymized = Promise.all(myComments.docs.map((doc) =>
      writer.update(doc.ref, {
        content: DELETED_COMMENT_CONTENT,
        user: ANONYMOUS_COMMENT_USER,
      }),
    ));
    await writer.flush();
    await anonymized;

    // 5. 남의 게시글의 내 좋아요 제거 (+ 예전 방식의 likes/{uid} 문서)
    const likedPosts = await posts
        .where("likedBy", "array-contains", uid).get();
    await runInChunks(likedPosts.docs, TRANSACTION_CHUNK_SIZE, (post) =>
      removeFromPostArray(post.ref, uid, "likedBy", "likeCount"),
    );
    await deleteRefs(
        writer,
        likedPosts.docs.map((post) => post.ref.collection("likes").doc(uid)),
    );

    // 6. 남의 게시글의 내 북마크 제거
    const bookmarkedPosts = await posts
        .where("bookmarkedBy", "array-contains", uid).get();
    await runInChunks(bookmarkedPosts.docs, TRANSACTION_CHUNK_SIZE, (post) =>
      removeFromPostArray(post.ref, uid, "bookmarkedBy", "bookmarkCount"),
    );

    // 7. 실시간 학습 현황 (학습 중 비정상 종료 후 탈퇴해도 남지 않게)
    await deleteRefs(writer, [db.collection("studying").doc(uid)]);

    // 8. 닉네임 선점 문서: users의 nickname 대신 uid로 찾는다
    //    → users를 이미 지운 뒤 재시도해도, 예전에 남은 닉네임 문서가 있어도 모두 지워진다
    const myNicknames = await db.collection("nicknames")
        .where("uid", "==", uid).get();
    await deleteRefs(writer, myNicknames.docs.map((doc) => doc.ref));

    // 9. users/{uid}는 데이터 중 마지막 (남은 하위 컬렉션이 있으면 함께 삭제)
    await db.recursiveDelete(userRef, writer);
  } finally {
    await writer.close();
  }
}

/**
 * Firebase Auth 계정을 지운다. 이미 없으면(이전 호출에서 삭제됨) 성공으로 본다.
 * @param {string} uid
 * @return {Promise<void>}
 */
async function deleteAuthUser(uid) {
  try {
    await getAuth().deleteUser(uid);
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      logger.info("Auth 계정이 이미 없어 삭제를 건너뜀", {uid});
      return;
    }
    throw error;
  }
}

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
    await deleteUserData(uid);
    await deleteAuthUser(uid);
  } catch (error) {
    // 원인은 로그로만 남기고, 앱에는 재시도 안내만 보낸다 (내부 정보 노출 방지)
    logger.error("회원탈퇴 처리 실패", {uid, error});
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

/**
 * studying 문서가 오래됐는지 (updatedAt이 없거나 Timestamp가 아니어도 오래된 것으로 본다).
 * @param {*} updatedAt 문서의 updatedAt 값
 * @param {number} cutoffMs 이 시각(ms)보다 이전이면 오래된 것
 * @return {boolean}
 */
function isStaleStudying(updatedAt, cutoffMs) {
  if (!(updatedAt instanceof Timestamp)) return true;
  return updatedAt.toMillis() < cutoffMs;
}

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
    logger.error("오래된 studying 문서 일부 삭제 실패 (다음 실행에서 다시 처리)", {
      deleted, skipped, failed: failures.length,
      error: failures[0].reason,
    });
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
