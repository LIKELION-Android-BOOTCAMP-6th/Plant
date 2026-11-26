// 메일 요청 처리용 운영자 스크립트(scripts/deleteAccountByEmail.js) 통합 테스트 (#778)
// 에뮬레이터에서 실행된다: npm --prefix functions test
// 스크립트의 deleteAccountByEmail을 에뮬레이터 Firestore·Auth로 직접 부른다
// (명령줄 입력 대신 confirm 함수로 uid 입력을 흉내 낸다)
const {describe, it, before} = require("node:test");
const assert = require("node:assert");
const {Timestamp} = require("firebase-admin/firestore");
const {
  auth, clearAuth, clearFirestore, db, uniqueId,
} = require("../helpers/emulator");
const {deleteAccountByEmail} = require("../../scripts/deleteAccountByEmail");
const {
  ANONYMOUS_COMMENT_USER,
  DELETED_COMMENT_CONTENT,
} = require("../../accountDeletion");

const users = db.collection("users");
const posts = db.collection("posts");
const activities = db.collection("activities");

/**
 * 입력 확인에 대상 uid를 그대로 넣는 confirm (실제 삭제 진행).
 * @param {Object} account
 * @return {Promise<string>}
 */
async function typeUid(account) {
  return account.uid;
}

/**
 * 호출되면 실패하는 confirm (dry-run·계정 없음에서는 입력을 받지 않아야 한다).
 * @return {Promise<string>}
 */
async function mustNotConfirm() {
  throw new Error("confirm이 호출되면 안 됨");
}

/**
 * 스크립트를 부르고 출력 줄도 함께 돌려준다.
 * @param {string} email
 * @param {Object} [options] dryRun, confirm
 * @return {Promise<{result: Object, lines: string[]}>}
 */
async function run(email, {dryRun = false, confirm = typeUid} = {}) {
  const lines = [];
  const result = await deleteAccountByEmail({
    db, auth, email, dryRun, confirm, print: (line) => lines.push(line),
  });
  return {result, lines};
}

/**
 * Auth 계정을 만든다 (Google 로그인 계정처럼 이메일이 있는 계정).
 * @param {string} uid
 * @return {Promise<string>} email
 */
async function createAuthUser(uid) {
  const email = `${uid}@example.com`;
  await auth.createUser({uid, email});
  return email;
}

/**
 * Auth 계정이 없는지 확인한다.
 * @param {string} uid
 * @return {Promise<void>}
 */
async function assertAuthDeleted(uid) {
  await assert.rejects(
      auth.getUser(uid),
      (error) => error.code === "auth/user-not-found",
  );
}

/**
 * 가입을 마친 사용자의 데이터를 앱과 같은 모양으로 만든다.
 * 남(other)의 게시글에 남긴 댓글·좋아요·북마크와, 남이 내 게시글에 남긴 댓글도 만든다.
 * @param {string} uid
 * @param {string} other
 * @return {Promise<Object>} 확인에 쓸 문서 참조들
 */
async function seedCompletedUser(uid, other) {
  const nickname = `nick-${uid}`;
  const userRef = users.doc(uid);
  const potRef = userRef.collection("pots").doc(uniqueId("pot"));
  const logRef = potRef.collection("logs").doc(uniqueId("log"));
  const blockRef = userRef.collection("blocks").doc(other);
  const nicknameRef = db.collection("nicknames").doc(nickname);

  // 내 게시글 + 남이 단 댓글 + 그 댓글의 activity(남의 것이지만 대상 글이 없어지므로 삭제)
  const myPostRef = posts.doc(uniqueId("post"));
  const otherCommentOnMyPostRef =
    myPostRef.collection("comments").doc(uniqueId("comment"));
  const otherActivityOnMyPostRef = activities.doc(uniqueId("activity"));

  // 남의 게시글 + 내 댓글·좋아요·북마크 + 내 activity
  const otherPostRef = posts.doc(uniqueId("post"));
  const myCommentRef =
    otherPostRef.collection("comments").doc(uniqueId("comment"));
  const myActivityRef = activities.doc(uniqueId("activity"));
  const otherOwnActivityRef = activities.doc(uniqueId("activity"));

  const reportRef = db.collection("reports").doc(`${uid}_${otherPostRef.id}`);
  const feedbackRef = db.collection("feedbacks").doc(uniqueId("feedback"));
  const studyingRef = db.collection("studying").doc(uid);

  await Promise.all([
    userRef.set({
      nickname, profileImg: "", isFirstLogin: false, totalStudyTime: 0,
      consentedAt: Timestamp.now(),
    }),
    nicknameRef.set({nickname, uid}),
    potRef.set({name: "화분", potTotalStudyingTime: 0}),
    logRef.set({title: "공부", studyingTime: 0}),
    blockRef.set({nickname: "남", profileImg: ""}),
    myPostRef.set({
      author: {id: uid, nickname}, title: "내 글", commentCount: 1,
      likeCount: 0, likedBy: [], bookmarkCount: 0, bookmarkedBy: [],
    }),
    otherCommentOnMyPostRef.set({
      user: {uid: other, nickname: "남", profileImg: ""}, content: "남의 댓글",
    }),
    otherActivityOnMyPostRef.set({
      uid: other, type: "COMMENT", targetId: myPostRef.id,
    }),
    otherPostRef.set({
      author: {id: other, nickname: "남"}, title: "남의 글", commentCount: 1,
      likeCount: 2, likedBy: [uid, other],
      bookmarkCount: 1, bookmarkedBy: [uid],
    }),
    myCommentRef.set({
      user: {uid, nickname, profileImg: ""}, content: "내 댓글",
    }),
    myActivityRef.set({uid, type: "COMMENT", targetId: otherPostRef.id}),
    otherOwnActivityRef.set(
        {uid: other, type: "POST", targetId: otherPostRef.id},
    ),
    reportRef.set({
      targetType: "post", targetId: otherPostRef.id, reporterId: uid,
      reason: "spam", detail: "", status: "pending",
    }),
    feedbackRef.set({
      type: "bug", reporterId: uid, description: "버그",
      deviceModel: "test-model", androidVersion: "34", appVersion: "1.0.0",
    }),
    studyingRef.set({nickname, updatedAt: Timestamp.now()}),
  ]);

  return {
    userRef, potRef, logRef, blockRef, nicknameRef, myPostRef,
    otherCommentOnMyPostRef, otherActivityOnMyPostRef, otherPostRef,
    myCommentRef, myActivityRef, otherOwnActivityRef, reportRef, feedbackRef,
    studyingRef,
  };
}

/**
 * 문서가 없는지 확인한다.
 * @param {FirebaseFirestore.DocumentReference[]} refs
 * @return {Promise<void>}
 */
async function assertAllDeleted(refs) {
  const snapshots = await Promise.all(refs.map((ref) => ref.get()));
  const remaining = snapshots.filter((s) => s.exists).map((s) => s.ref.path);
  assert.deepStrictEqual(remaining, [], "남아 있는 문서");
}

describe("deleteAccountByEmail 운영자 스크립트 (#778)", () => {
  before(async () => {
    await Promise.all([clearFirestore(), clearAuth()]);
  });

  it("가입을 마친 계정은 앱 탈퇴와 같은 범위로 지우고 Auth 계정도 지운다", async () => {
    const uid = uniqueId("user");
    const other = uniqueId("other");
    const email = await createAuthUser(uid);
    const otherEmail = await createAuthUser(other);
    await users.doc(other).set({nickname: "남", isFirstLogin: false});
    const refs = await seedCompletedUser(uid, other);

    const {result, lines} = await run(email);

    assert.deepStrictEqual(result, {status: "deleted", uid});
    // 출력에 확인용 정보(uid·이메일·닉네임·가입 상태)가 있다
    const output = lines.join("\n");
    assert.ok(output.includes(uid));
    assert.ok(output.includes(email));
    assert.ok(output.includes(`nick-${uid}`));
    assert.ok(output.includes("가입 완료"));

    // 지워지는 문서
    await assertAllDeleted([
      refs.userRef, refs.potRef, refs.logRef, refs.blockRef, refs.nicknameRef,
      refs.myPostRef, refs.otherCommentOnMyPostRef,
      refs.otherActivityOnMyPostRef, refs.myActivityRef, refs.studyingRef,
      refs.reportRef,
    ]);
    await assertAuthDeleted(uid);

    // 남의 게시글에 남긴 댓글은 익명화
    assert.deepStrictEqual((await refs.myCommentRef.get()).data(), {
      user: ANONYMOUS_COMMENT_USER, content: DELETED_COMMENT_CONTENT,
    });
    // 남의 게시글의 좋아요·북마크에서 빠지고 개수가 준다
    const otherPost = (await refs.otherPostRef.get()).data();
    assert.deepStrictEqual(otherPost.likedBy, [other]);
    assert.strictEqual(otherPost.likeCount, 1);
    assert.deepStrictEqual(otherPost.bookmarkedBy, []);
    assert.strictEqual(otherPost.bookmarkCount, 0);
    // 신고는 익명 사본으로, 제보는 작성자·기기 정보만 지움
    const reportCopies = await db.collection("reports")
        .where("targetId", "==", refs.otherPostRef.id).get();
    assert.strictEqual(reportCopies.size, 1);
    assert.strictEqual(reportCopies.docs[0].get("reporterId"), "");
    assert.deepStrictEqual((await refs.feedbackRef.get()).data(), {
      type: "bug", reporterId: "", description: "버그", appVersion: "1.0.0",
    });

    // 남의 데이터는 그대로
    assert.ok((await refs.otherOwnActivityRef.get()).exists);
    assert.ok((await users.doc(other).get()).exists);
    assert.strictEqual((await auth.getUserByEmail(otherEmail)).uid, other);
  });

  it("users 문서가 없는 계정(가입 전)은 Auth 계정만 지우고 오류 없이 끝난다", async () => {
    const uid = uniqueId("user");
    const email = await createAuthUser(uid);

    const {result, lines} = await run(email);

    assert.deepStrictEqual(result, {status: "deleted", uid});
    assert.ok(lines.join("\n").includes("users 문서 없음"));
    await assertAuthDeleted(uid);
  });

  it("users 문서만 일부 있는 계정(닉네임 설정 전)도 지운다", async () => {
    const uid = uniqueId("user");
    const email = await createAuthUser(uid);
    // 첫 로그인 직후 상태: 닉네임 설정·동의 전 (nicknames 문서 없음)
    const userRef = users.doc(uid);
    await userRef.set({nickname: "", isFirstLogin: true});

    const {result, lines} = await run(email);

    assert.deepStrictEqual(result, {status: "deleted", uid});
    assert.ok(lines.join("\n").includes("닉네임 설정 전"));
    await assertAllDeleted([userRef]);
    await assertAuthDeleted(uid);
  });

  it("이메일로 찾은 계정이 없으면 아무것도 지우지 않고 계정 없음을 알린다", async () => {
    const other = uniqueId("other");
    const otherEmail = await createAuthUser(other);
    await users.doc(other).set({nickname: "남", isFirstLogin: false});

    const {result, lines} = await run(`${uniqueId("nobody")}@example.com`, {
      confirm: mustNotConfirm,
    });

    assert.deepStrictEqual(result, {status: "not-found", uid: undefined});
    assert.ok(lines.join("\n").includes("계정 없음"));
    assert.ok((await users.doc(other).get()).exists);
    assert.strictEqual((await auth.getUserByEmail(otherEmail)).uid, other);
  });

  it("dry-run은 대상만 보여 주고 아무것도 지우지 않는다", async () => {
    const uid = uniqueId("user");
    const other = uniqueId("other");
    const email = await createAuthUser(uid);
    const refs = await seedCompletedUser(uid, other);

    const {result, lines} = await run(email, {
      dryRun: true, confirm: mustNotConfirm,
    });

    assert.deepStrictEqual(result, {status: "dry-run", uid});
    const output = lines.join("\n");
    assert.ok(output.includes(uid));
    assert.ok(output.includes("화분 1개 · 게시글 1개 · 댓글 1개"));
    assert.ok((await refs.userRef.get()).exists);
    assert.strictEqual(
        (await refs.myCommentRef.get()).get("content"), "내 댓글",
    );
    assert.strictEqual((await auth.getUser(uid)).uid, uid);
  });

  it("확인 입력이 uid와 다르면 취소하고 아무것도 지우지 않는다", async () => {
    const uid = uniqueId("user");
    const email = await createAuthUser(uid);
    const userRef = users.doc(uid);
    await userRef.set({nickname: `nick-${uid}`, isFirstLogin: false});

    // 이메일·닉네임 등 uid가 아닌 값, 앞부분만 맞는 값은 모두 취소
    for (const typed of [email, `nick-${uid}`, uid.slice(0, -1), "", "yes"]) {
      const {result} = await run(email, {confirm: async () => typed});
      assert.deepStrictEqual(result, {status: "cancelled", uid}, typed);
    }

    assert.ok((await userRef.get()).exists);
    assert.strictEqual((await auth.getUser(uid)).uid, uid);
  });

  it("중간에 실패한 뒤 다시 실행하면 남은 데이터를 지우고, 끝난 뒤 다시 실행하면 계정 없음", async () => {
    const uid = uniqueId("user");
    const other = uniqueId("other");
    const email = await createAuthUser(uid);
    const refs = await seedCompletedUser(uid, other);
    // users 문서만 먼저 없어지고 나머지 데이터·Auth 계정은 그대로인, 가장 불리한 상태로
    // 재실행을 확인한다 (실제 중간 실패는 users 문서를 마지막에 지우므로 이보다 남은 것이 적다)
    await refs.userRef.delete();

    const retried = await run(email);

    assert.deepStrictEqual(retried.result, {status: "deleted", uid});
    await assertAllDeleted([
      refs.potRef, refs.logRef, refs.blockRef, refs.nicknameRef,
      refs.myPostRef, refs.myActivityRef, refs.studyingRef, refs.reportRef,
    ]);
    assert.strictEqual(
        (await refs.myCommentRef.get()).get("content"), DELETED_COMMENT_CONTENT,
    );
    await assertAuthDeleted(uid);

    const again = await run(email, {confirm: mustNotConfirm});

    assert.deepStrictEqual(again.result, {status: "not-found", uid: undefined});
  });
});
