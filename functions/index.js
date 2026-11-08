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
const logger = require("firebase-functions/logger");
const {initializeApp} = require("firebase-admin/app");
const {getAuth} = require("firebase-admin/auth");
const {FieldValue, getFirestore} = require("firebase-admin/firestore");

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
