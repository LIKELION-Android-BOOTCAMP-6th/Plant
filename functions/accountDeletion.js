// 회원탈퇴 데이터 삭제 (#457, #606, #778)
// index.js의 deleteAccount(앱 탈퇴)와 운영자용 스크립트
// scripts/deleteAccountByEmail.js(메일 요청 처리)가 같은 범위로 지우도록 함께 쓴다.
// 스크립트는 index.js를 불러오면 함수 정의·initializeApp까지 실행되므로 따로 뺐다
// → Firestore·Auth 인스턴스(db, auth)는 인자로 받는다 (initializeApp 하지 않음)
// - 모든 단계는 다시 실행해도 안전하다(멱등). 중간에 실패하면 같은 함수를 다시 부르면 되고,
//   이미 지운 문서는 조회되지 않으므로 남은 단계부터 이어서 처리된다
// - 순서: 데이터 → 닉네임 → users/{uid} → Auth 계정
//   Auth 계정이 먼저 사라지면 앱이 다시 호출할 수 없으므로 반드시 마지막에 지운다
// - Admin SDK는 보안 규칙의 영향을 받지 않는다 → #431에서 탈퇴용으로 열어둔 규칙 예외를 제거함
const logger = require("firebase-functions/logger");
const {FieldValue} = require("firebase-admin/firestore");
// 탈퇴 시 신고 익명화는 경쟁 상황을 테스트에서 직접 확인하려고 따로 뺐다 (#606)
const {
  ANONYMOUS_REPORTER_ID,
  moveReportsAnonymously,
} = require("./reportAnonymizer");

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
 * @param {FirebaseFirestore.Firestore} db
 * @param {FirebaseFirestore.DocumentReference} postRef
 * @param {string} uid
 * @param {string} arrayField likedBy | bookmarkedBy
 * @param {string} countField likeCount | bookmarkCount
 * @return {Promise<void>}
 */
async function removeFromPostArray(db, postRef, uid, arrayField, countField) {
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
 * users/{uid}가 없는 계정(가입을 마치지 않음)도 오류 없이 있는 데이터만 지운다.
 * @param {FirebaseFirestore.Firestore} db
 * @param {string} uid
 * @return {Promise<void>}
 */
async function deleteUserData(db, uid) {
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
      removeFromPostArray(db, post.ref, uid, "likedBy", "likeCount"),
    );
    await deleteRefs(
        writer,
        likedPosts.docs.map((post) => post.ref.collection("likes").doc(uid)),
    );

    // 6. 남의 게시글의 내 북마크 제거
    const bookmarkedPosts = await posts
        .where("bookmarkedBy", "array-contains", uid).get();
    await runInChunks(bookmarkedPosts.docs, TRANSACTION_CHUNK_SIZE, (post) =>
      removeFromPostArray(db, post.ref, uid, "bookmarkedBy", "bookmarkCount"),
    );

    // 7. 실시간 학습 현황 (학습 중 비정상 종료 후 탈퇴해도 남지 않게)
    await deleteRefs(writer, [db.collection("studying").doc(uid)]);

    // 8. 내가 한 신고 익명화 (#606)
    //    지우지 않고 새 ID로 옮겨 reporterId만 비운다 → 운영진의 pending 검토와
    //    허위 신고 기각(status → dismissed, reportCount - 1)을 탈퇴 뒤에도 할 수 있다
    //    대상 게시글·댓글의 reportCount는 그대로 둔다 (탈퇴로 블라인드가 풀리지 않게)
    //    옮긴 사본은 reporterId가 ""라 재시도 때 다시 조회되지 않는다
    const myReports = await db.collection("reports")
        .where("reporterId", "==", uid).get();
    await moveReportsAnonymously(db, myReports.docs);

    // 9. 내 버그 제보·개선 제안 익명화 (#606)
    //    내용·category·appVersion은 버그 분석용으로 남기고 작성자와 기기 정보만 지운다
    //    익명화하면 reporterId가 ""가 되어 재시도 때 다시 조회되지 않는다
    const myFeedbacks = await db.collection("feedbacks")
        .where("reporterId", "==", uid).get();
    const anonymizedFeedbacks = Promise.all(myFeedbacks.docs.map((doc) =>
      writer.update(doc.ref, {
        reporterId: ANONYMOUS_REPORTER_ID,
        deviceModel: FieldValue.delete(),
        androidVersion: FieldValue.delete(),
      }),
    ));
    await writer.flush();
    await anonymizedFeedbacks;

    // 10. 닉네임 선점 문서: users의 nickname 대신 uid로 찾는다
    //    → users를 이미 지운 뒤 재시도해도, 예전에 남은 닉네임 문서가 있어도 모두 지워진다
    const myNicknames = await db.collection("nicknames")
        .where("uid", "==", uid).get();
    await deleteRefs(writer, myNicknames.docs.map((doc) => doc.ref));

    // 11. users/{uid}는 데이터 중 마지막 (남은 하위 컬렉션이 있으면 함께 삭제)
    await db.recursiveDelete(userRef, writer);
  } finally {
    await writer.close();
  }
}

/**
 * Firebase Auth 계정을 지운다. 이미 없으면(이전 호출에서 삭제됨) 성공으로 본다.
 * @param {Object} auth Admin SDK의 Auth (getAuth()로 받은 값)
 * @param {string} uid
 * @return {Promise<void>}
 */
async function deleteAuthUser(auth, uid) {
  try {
    await auth.deleteUser(uid);
  } catch (error) {
    if (error.code === "auth/user-not-found") {
      logger.info("Auth 계정이 이미 없어 삭제를 건너뜀", {uid});
      return;
    }
    throw error;
  }
}

module.exports = {
  DELETED_COMMENT_CONTENT,
  ANONYMOUS_COMMENT_USER,
  deleteUserData,
  deleteAuthUser,
};
