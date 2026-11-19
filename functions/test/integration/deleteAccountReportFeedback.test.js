// deleteAccount의 신고(reports) 삭제·제보(feedbacks) 익명화 단계 통합 테스트 (#606)
// 에뮬레이터에서 실행된다: npm --prefix functions test
// 기존 탈퇴 단계(화분·게시글·댓글 등) 전체 테스트는 #620 범위라 여기서는 추가한 두 단계만 확인한다
// ⚠️ deleteAccount는 Auth 계정을 지우므로, callFunction에 uid 대신
//    signIn으로 받은 idToken을 넘긴다
//    (uid로 다시 부르면 Auth 에뮬레이터가 지운 계정을 새로 만든다)
const {describe, it, before} = require("node:test");
const assert = require("node:assert");
const {FieldPath, Timestamp} = require("firebase-admin/firestore");
const {
  auth, callFunction, clearAuth, clearFirestore, db, signIn, uniqueId,
} = require("../helpers/emulator");

const reports = db.collection("reports");
const feedbacks = db.collection("feedbacks");
const posts = db.collection("posts");

/**
 * 앱(CommunityReportRemoteDataSourceImpl)과 같은 모양의 신고 문서를 만든다.
 * 문서 ID는 규칙과 같이 {신고자uid}_{대상ID}다.
 * @param {string} reporterId
 * @param {string} targetId
 * @param {Object} [fields] targetType·parentPostId 등 덮어쓸 값
 * @return {{ref: FirebaseFirestore.DocumentReference, data: Object}}
 */
function reportDoc(reporterId, targetId, fields = {}) {
  return {
    ref: reports.doc(`${reporterId}_${targetId}`),
    data: {
      targetType: "post",
      targetId,
      parentPostId: targetId,
      reporterId,
      reason: "spam",
      detail: "광고 글이에요",
      createdAt: Timestamp.now(),
      status: "pending",
      ...fields,
    },
  };
}

/**
 * 앱(FeedbackMapper)과 같은 모양의 버그 제보 문서 데이터를 만든다.
 * @param {string} reporterId
 * @return {Object}
 */
function bugFeedback(reporterId) {
  return {
    type: "bug",
    reporterId,
    category: "ui_bug",
    description: "버튼이 안 눌려요",
    reproductionSteps: "홈에서 버튼을 누름",
    deviceModel: "test-model",
    androidVersion: "34",
    appVersion: "1.0.0",
    createdAt: Timestamp.now(),
    status: "pending",
  };
}

/**
 * 앱(FeedbackMapper)과 같은 모양의 개선 제안 문서 데이터를 만든다 (기기 정보 없음).
 * @param {string} reporterId
 * @return {Object}
 */
function improvementFeedback(reporterId) {
  return {
    type: "improvement",
    reporterId,
    category: "new_feature",
    description: "다크모드 색을 더 어둡게 해주세요",
    appVersion: "1.0.0",
    createdAt: Timestamp.now(),
    status: "pending",
  };
}

/**
 * 버그 제보를 익명화했을 때 기대하는 데이터 (작성자·기기 정보만 빠지고 나머지는 그대로).
 * @param {Object} data bugFeedback으로 만든 원래 데이터
 * @return {Object}
 */
function anonymizedBug(data) {
  const expected = {...data, reporterId: ""};
  delete expected.deviceModel;
  delete expected.androidVersion;
  return expected;
}

/**
 * 탈퇴할 사용자를 만들고, 재인증 직후처럼 auth_time이 지금인 토큰을 받는다.
 * @param {string} uid
 * @return {Promise<string>} idToken
 */
async function prepareUser(uid) {
  await db.collection("users").doc(uid).set({nickname: uid});
  return signIn(uid);
}

/**
 * uid가 신고한 문서를 reporterId 조건과 문서 ID 접두사({uid}_) 두 가지로 센다.
 * @param {string} uid
 * @return {Promise<{byField: number, byId: number}>}
 */
async function countReportsOf(uid) {
  const [byField, byId] = await Promise.all([
    reports.where("reporterId", "==", uid).get(),
    reports.orderBy(FieldPath.documentId())
        .startAt(`${uid}_`).endAt(`${uid}_`).get(),
  ]);
  return {byField: byField.size, byId: byId.size};
}

/**
 * 탈퇴가 끝까지 진행됐는지(users·Auth 계정 삭제) 확인한다.
 * @param {string} uid
 * @return {Promise<void>}
 */
async function assertAccountDeleted(uid) {
  const user = await db.collection("users").doc(uid).get();
  assert.strictEqual(user.exists, false, "users 문서가 남아 있음");
  await assert.rejects(
      auth.getUser(uid),
      (error) => error.code === "auth/user-not-found",
  );
}

describe("deleteAccount 신고·제보 정리 (#606)", () => {
  before(async () => {
    await Promise.all([clearFirestore(), clearAuth()]);
  });

  it("내 신고는 삭제하고 내 제보는 익명화하며, 남의 문서와 reportCount는 그대로 둔다", async () => {
    const uid = uniqueId("user");
    const other = uniqueId("other");
    const idToken = await prepareUser(uid);

    // 남이 쓴 게시글·댓글을 내가 신고함 (신고로 reportCount가 올라간 상태)
    const postId = uniqueId("post");
    const postRef = posts.doc(postId);
    const commentRef = postRef.collection("comments").doc(uniqueId("comment"));
    await postRef.set({author: {id: other}, title: "남의 글", reportCount: 3});
    await commentRef.set(
        {user: {uid: other}, content: "남의 댓글", reportCount: 2},
    );

    const myPostReport = reportDoc(uid, postId);
    const myCommentReport = reportDoc(uid, commentRef.id, {
      targetType: "comment", parentPostId: postId,
    });
    // 같은 글을 다른 사람도 신고함 → 신고자의 데이터라 남아야 한다
    const otherReport = reportDoc(other, postId);

    const myBugRef = feedbacks.doc(uniqueId("feedback"));
    const myImprovementRef = feedbacks.doc(uniqueId("feedback"));
    const otherBugRef = feedbacks.doc(uniqueId("feedback"));
    const myBug = bugFeedback(uid);
    const myImprovement = improvementFeedback(uid);
    const otherBug = bugFeedback(other);

    await Promise.all([
      myPostReport.ref.set(myPostReport.data),
      myCommentReport.ref.set(myCommentReport.data),
      otherReport.ref.set(otherReport.data),
      myBugRef.set(myBug),
      myImprovementRef.set(myImprovement),
      otherBugRef.set(otherBug),
    ]);

    const result = await callFunction("deleteAccount", {}, {idToken});

    assert.deepStrictEqual(result, {deleted: true});
    // 신고: 내 것은 필드·문서 ID 어느 쪽으로도 남지 않고, 남의 신고는 그대로
    assert.deepStrictEqual(await countReportsOf(uid), {byField: 0, byId: 0});
    assert.deepStrictEqual(
        (await otherReport.ref.get()).data(), otherReport.data,
    );
    // reportCount는 집계 값이라 탈퇴 전과 같다 (블라인드 유지)
    assert.strictEqual((await postRef.get()).get("reportCount"), 3);
    assert.strictEqual((await commentRef.get()).get("reportCount"), 2);
    // 제보: 내 것은 작성자·기기 정보만 빠지고 내용은 남는다, 남의 제보는 그대로
    const myFeedbacks = await feedbacks.where("reporterId", "==", uid).get();
    assert.strictEqual(myFeedbacks.size, 0);
    assert.deepStrictEqual((await myBugRef.get()).data(), anonymizedBug(myBug));
    assert.deepStrictEqual(
        (await myImprovementRef.get()).data(),
        {...myImprovement, reporterId: ""},
    );
    assert.deepStrictEqual((await otherBugRef.get()).data(), otherBug);
    await assertAccountDeleted(uid);
  });

  it("신고·제보가 없는 사용자도 기존처럼 탈퇴된다", async () => {
    const uid = uniqueId("user");
    const idToken = await prepareUser(uid);

    const result = await callFunction("deleteAccount", {}, {idToken});

    assert.deepStrictEqual(result, {deleted: true});
    await assertAccountDeleted(uid);
  });

  it("중간에 실패한 뒤 다시 호출하면 남은 신고·제보만 이어서 처리한다", async () => {
    const uid = uniqueId("user");
    const idToken = await prepareUser(uid);

    // 지난 호출이 신고 하나를 지우고, 제보 하나를 익명화한 뒤 실패한 상태
    // (users 문서·Auth 계정과 나머지 신고·제보는 남아 있음)
    const remainingReport = reportDoc(uid, uniqueId("post"));
    const remainingBugRef = feedbacks.doc(uniqueId("feedback"));
    const doneBugRef = feedbacks.doc(uniqueId("feedback"));
    const remainingBug = bugFeedback(uid);
    const doneBug = anonymizedBug(bugFeedback(uid));
    await Promise.all([
      remainingReport.ref.set(remainingReport.data),
      remainingBugRef.set(remainingBug),
      doneBugRef.set(doneBug),
    ]);

    const retried = await callFunction("deleteAccount", {}, {idToken});

    assert.deepStrictEqual(retried, {deleted: true});
    assert.deepStrictEqual(await countReportsOf(uid), {byField: 0, byId: 0});
    assert.deepStrictEqual(
        (await remainingBugRef.get()).data(), anonymizedBug(remainingBug),
    );
    // 이미 익명화한 제보는 다시 조회되지 않아 그대로다
    assert.deepStrictEqual((await doneBugRef.get()).data(), doneBug);
    await assertAccountDeleted(uid);

    // 모두 끝난 뒤 한 번 더 불러도 성공하고 결과가 같다 (멱등)
    const again = await callFunction("deleteAccount", {}, {idToken});

    assert.deepStrictEqual(again, {deleted: true});
    assert.deepStrictEqual(
        (await remainingBugRef.get()).data(), anonymizedBug(remainingBug),
    );
    assert.deepStrictEqual((await doneBugRef.get()).data(), doneBug);
  });

  it("신고·제보가 batch 한도(500건)보다 많아도 모두 처리한다", async () => {
    const uid = uniqueId("user");
    const idToken = await prepareUser(uid);
    const COUNT = 510;

    const seedWriter = db.bulkWriter();
    for (let i = 0; i < COUNT; i++) {
      const report = reportDoc(uid, `post-${i}`);
      seedWriter.set(report.ref, report.data);
      seedWriter.set(feedbacks.doc(`${uid}-feedback-${i}`), bugFeedback(uid));
    }
    await seedWriter.close();

    const result = await callFunction("deleteAccount", {}, {idToken});

    assert.deepStrictEqual(result, {deleted: true});
    assert.deepStrictEqual(await countReportsOf(uid), {byField: 0, byId: 0});
    assert.strictEqual(
        (await feedbacks.where("reporterId", "==", uid).get()).size, 0,
    );
    // 익명화된 제보 중 기기 정보가 남은 문서가 없다
    const seeded = await feedbacks.orderBy(FieldPath.documentId())
        .startAt(`${uid}-feedback-`).endAt(`${uid}-feedback-`).get();
    assert.strictEqual(seeded.size, COUNT);
    const withDevice = seeded.docs.filter((doc) =>
      doc.get("deviceModel") !== undefined ||
      doc.get("androidVersion") !== undefined,
    );
    assert.strictEqual(withDevice.length, 0);
    await assertAccountDeleted(uid);
  });
});
