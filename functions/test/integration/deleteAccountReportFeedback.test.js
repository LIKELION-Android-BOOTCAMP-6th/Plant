// deleteAccount의 신고(reports)·제보(feedbacks) 익명화 단계 통합 테스트 (#606)
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

// 문자열 범위 조회의 끝값 (접두사로 시작하는 ID·값을 모두 포함)
const RANGE_END = "";

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
        .startAt(`${uid}_`).endAt(`${uid}_${RANGE_END}`).get(),
  ]);
  return {byField: byField.size, byId: byId.size};
}

/**
 * 대상(targetId)에 대한 익명 신고 사본(reporterId == "")을 모두 읽는다.
 * @param {string} targetId
 * @return {Promise<FirebaseFirestore.QueryDocumentSnapshot[]>}
 */
async function anonymousCopiesOf(targetId) {
  const snapshot = await reports.where("targetId", "==", targetId).get();
  return snapshot.docs.filter((doc) => doc.get("reporterId") === "");
}

/**
 * 대상의 익명 사본이 정확히 1개이고, 원본에서 reporterId만 비운 값인지 확인한다.
 * 사본의 문서 ID에 uid가 들어 있지 않은지도 확인한다.
 * @param {string} uid
 * @param {Object} original reportDoc으로 만든 원래 데이터
 * @return {Promise<void>}
 */
async function assertMovedAnonymously(uid, original) {
  const copies = await anonymousCopiesOf(original.targetId);
  assert.strictEqual(copies.length, 1, `${original.targetId} 사본 개수`);
  assert.deepStrictEqual(copies[0].data(), {...original, reporterId: ""});
  assert.ok(!copies[0].id.includes(uid), `사본 ID에 uid: ${copies[0].id}`);
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

describe("deleteAccount 신고·제보 익명화 (#606)", () => {
  before(async () => {
    await Promise.all([clearFirestore(), clearAuth()]);
  });

  it("내 신고·제보는 익명화하고, 남의 문서와 reportCount는 그대로 둔다", async () => {
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
    // 운영진이 이미 처리한 신고 → 옮긴 뒤에도 처리 결과(status)가 남아야 한다
    const myCommentReport = reportDoc(uid, commentRef.id, {
      targetType: "comment", parentPostId: postId, status: "dismissed",
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
    // 신고: 내 원본은 필드·문서 ID 어느 쪽으로도 남지 않고, 익명 사본이 1개씩 생긴다
    assert.deepStrictEqual(await countReportsOf(uid), {byField: 0, byId: 0});
    await assertMovedAnonymously(uid, myPostReport.data);
    await assertMovedAnonymously(uid, myCommentReport.data);
    // 남의 신고는 그대로 (게시글 신고는 남의 원본 1개 + 내 사본 1개)
    assert.deepStrictEqual(
        (await otherReport.ref.get()).data(), otherReport.data,
    );
    const postReports = await reports.where("targetId", "==", postId).get();
    assert.strictEqual(postReports.size, 2);
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

  it("중간에 실패한 뒤 다시 호출하면 남은 신고·제보만 이어서 처리하고 사본이 늘지 않는다", async () => {
    const uid = uniqueId("user");
    const idToken = await prepareUser(uid);

    // 지난 호출이 신고 하나를 옮기고(원본 삭제 + 익명 사본), 제보 하나를 익명화한 뒤
    // 실패한 상태 (users 문서·Auth 계정과 나머지 신고·제보는 남아 있음)
    const doneReport = reportDoc(uid, uniqueId("post"));
    const doneCopy = {...doneReport.data, reporterId: ""};
    const remainingReport = reportDoc(uid, uniqueId("post"));
    const remainingBugRef = feedbacks.doc(uniqueId("feedback"));
    const doneBugRef = feedbacks.doc(uniqueId("feedback"));
    const remainingBug = bugFeedback(uid);
    const doneBug = anonymizedBug(bugFeedback(uid));
    await Promise.all([
      reports.doc().set(doneCopy),
      remainingReport.ref.set(remainingReport.data),
      remainingBugRef.set(remainingBug),
      doneBugRef.set(doneBug),
    ]);

    /**
     * 재시도 뒤 기대하는 상태: 신고 원본 없음, 신고 사본은 대상마다 1개, 제보는 익명화.
     * @return {Promise<void>}
     */
    async function assertProcessed() {
      assert.deepStrictEqual(await countReportsOf(uid), {byField: 0, byId: 0});
      await assertMovedAnonymously(uid, remainingReport.data);
      // 이미 옮긴 신고는 다시 조회되지 않아 사본이 그대로 1개다
      await assertMovedAnonymously(uid, doneReport.data);
      assert.deepStrictEqual(
          (await remainingBugRef.get()).data(), anonymizedBug(remainingBug),
      );
      // 이미 익명화한 제보는 다시 조회되지 않아 그대로다
      assert.deepStrictEqual((await doneBugRef.get()).data(), doneBug);
    }

    const retried = await callFunction("deleteAccount", {}, {idToken});

    assert.deepStrictEqual(retried, {deleted: true});
    await assertProcessed();
    await assertAccountDeleted(uid);

    // 모두 끝난 뒤 한 번 더 불러도 성공하고 결과가 같다 (멱등)
    const again = await callFunction("deleteAccount", {}, {idToken});

    assert.deepStrictEqual(again, {deleted: true});
    await assertProcessed();
  });

  it("신고·제보가 batch 한도(500건)보다 많아도 모두 처리한다", async () => {
    const uid = uniqueId("user");
    const idToken = await prepareUser(uid);
    // 신고는 250건씩 batch로 옮기므로 250·500 경계를 모두 넘는 수
    const COUNT = 510;
    const targetPrefix = `${uid}-post-`;

    const seedWriter = db.bulkWriter();
    for (let i = 0; i < COUNT; i++) {
      const report = reportDoc(uid, `${targetPrefix}${i}`);
      seedWriter.set(report.ref, report.data);
      seedWriter.set(feedbacks.doc(`${uid}-feedback-${i}`), bugFeedback(uid));
    }
    await seedWriter.close();

    const result = await callFunction("deleteAccount", {}, {idToken});

    assert.deepStrictEqual(result, {deleted: true});
    // 신고: 원본은 없고, 대상마다 익명 사본이 하나씩 (빠지거나 겹친 대상 없음)
    assert.deepStrictEqual(await countReportsOf(uid), {byField: 0, byId: 0});
    const copies = await reports
        .where("targetId", ">=", targetPrefix)
        .where("targetId", "<=", `${targetPrefix}${RANGE_END}`).get();
    assert.strictEqual(copies.size, COUNT);
    assert.ok(copies.docs.every((doc) => doc.get("reporterId") === ""));
    const targets = new Set(copies.docs.map((doc) => doc.get("targetId")));
    assert.strictEqual(targets.size, COUNT);
    // 제보: 작성자가 남은 문서가 없고, 기기 정보가 남은 문서도 없다
    assert.strictEqual(
        (await feedbacks.where("reporterId", "==", uid).get()).size, 0,
    );
    const seeded = await feedbacks.orderBy(FieldPath.documentId())
        .startAt(`${uid}-feedback-`)
        .endAt(`${uid}-feedback-${RANGE_END}`).get();
    assert.strictEqual(seeded.size, COUNT);
    const withDevice = seeded.docs.filter((doc) =>
      doc.get("deviceModel") !== undefined ||
      doc.get("androidVersion") !== undefined,
    );
    assert.strictEqual(withDevice.length, 0);
    await assertAccountDeleted(uid);
  });
});
