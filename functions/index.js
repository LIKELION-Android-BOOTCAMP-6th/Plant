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
const logger = require("firebase-functions/logger");
const {initializeApp} = require("firebase-admin/app");
const {getFirestore} = require("firebase-admin/firestore");

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
