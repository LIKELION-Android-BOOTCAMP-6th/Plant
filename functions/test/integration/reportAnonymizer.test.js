// moveReportsAnonymously 경쟁 상황 통합 테스트 (#606)
// 에뮬레이터에서 실행된다: npm --prefix functions test
// 함수 에뮬레이터는 같은 함수 요청을 하나씩 실행해서, deleteAccount를 동시에 두 번 불러도
// 두 호출이 겹치지 않는다 (2026-11-19 확인: 원본 삭제 조건을 빼도 Callable 동시 호출 테스트가 통과함).
// → 신고 익명화 함수를 Firestore 에뮬레이터에 직접 불러, "조회한 뒤 그 사이 문서가 바뀐" 상황을 만든다
const {describe, it, before} = require("node:test");
const assert = require("node:assert");
const {Timestamp} = require("firebase-admin/firestore");
const {
  REPORT_MOVE_BATCH_SIZE, moveReportsAnonymously,
} = require("../../reportAnonymizer");
const {clearFirestore, db, uniqueId} = require("../helpers/emulator");

const reports = db.collection("reports");

/**
 * reporterId가 uid인 신고 원본을 count개 만들고, 탈퇴 함수처럼 조회한 결과를 돌려준다.
 * 대상 ID는 {prefix}-{번호}다.
 * @param {string} uid
 * @param {number} count
 * @return {Promise<FirebaseFirestore.QueryDocumentSnapshot[]>}
 */
async function seedAndQuery(uid, count) {
  const writer = db.bulkWriter();
  for (let i = 0; i < count; i++) {
    const targetId = `${uid}-post-${i}`;
    writer.set(reports.doc(`${uid}_${targetId}`), {
      targetType: "post",
      targetId,
      parentPostId: null,
      reporterId: uid,
      reason: "spam",
      detail: null,
      createdAt: Timestamp.now(),
      status: "pending",
    });
  }
  await writer.close();
  return (await reports.where("reporterId", "==", uid).get()).docs;
}

/**
 * uid가 시드한 대상들의 익명 사본을 대상 ID별 개수로 센다.
 * @param {string} uid
 * @return {Promise<Map<string, number>>}
 */
async function countCopiesByTarget(uid) {
  const prefix = `${uid}-post-`;
  const snapshot = await reports
      .where("targetId", ">=", prefix)
      .where("targetId", "<=", `${prefix}`).get();
  const counts = new Map();
  for (const doc of snapshot.docs) {
    if (doc.get("reporterId") !== "") continue;
    const targetId = doc.get("targetId");
    counts.set(targetId, (counts.get(targetId) || 0) + 1);
  }
  return counts;
}

/**
 * uid의 원본 신고 개수.
 * @param {string} uid
 * @return {Promise<number>}
 */
async function countOriginals(uid) {
  return (await reports.where("reporterId", "==", uid).get()).size;
}

describe("moveReportsAnonymously (#606)", () => {
  before(async () => {
    await clearFirestore();
  });

  it("겹쳐 실행된 두 호출이 같은 조회 결과로 옮겨도 사본은 대상마다 1개다", async () => {
    const uid = uniqueId("user");
    // 두 탈퇴 호출이 둘 다 원본을 조회한 뒤 각자 옮기는 상황
    const snapshotA = await seedAndQuery(uid, 2);
    const snapshotB = (await reports.where("reporterId", "==", uid).get()).docs;

    await moveReportsAnonymously(db, snapshotA);
    // 나중 호출의 batch는 원본이 이미 없어 통째로 실패해야 한다 (사본도 만들지 않음)
    await assert.rejects(moveReportsAnonymously(db, snapshotB));

    assert.strictEqual(await countOriginals(uid), 0);
    const copies = await countCopiesByTarget(uid);
    assert.deepStrictEqual(
        [...copies.entries()].sort(),
        [[`${uid}-post-0`, 1], [`${uid}-post-1`, 1]],
    );
  });

  it("조회한 뒤 운영진이 status를 바꾸면 옮기지 않고, 다시 조회해 옮기면 바뀐 값이 남는다", async () => {
    const uid = uniqueId("user");
    const docs = await seedAndQuery(uid, 2);
    // 조회와 옮기기 사이에 운영진이 콘솔에서 처리 결과를 바꿈
    await docs[0].ref.update({status: "dismissed"});

    await assert.rejects(moveReportsAnonymously(db, docs));

    // batch가 통째로 실패 → 사본은 하나도 없고, 원본 2개와 운영진의 변경이 그대로 남는다
    assert.strictEqual((await countCopiesByTarget(uid)).size, 0);
    assert.strictEqual(await countOriginals(uid), 2);
    assert.strictEqual((await docs[0].ref.get()).get("status"), "dismissed");

    // 앱이 다시 호출하면 새로 조회해서 옮긴다
    const requeried = (await reports.where("reporterId", "==", uid).get()).docs;
    await moveReportsAnonymously(db, requeried);

    assert.strictEqual(await countOriginals(uid), 0);
    const moved = await reports
        .where("targetId", "==", docs[0].get("targetId")).get();
    assert.strictEqual(moved.size, 1);
    assert.strictEqual(moved.docs[0].get("reporterId"), "");
    assert.strictEqual(moved.docs[0].get("status"), "dismissed");
  });

  it("뒤 묶음이 실패하면 앞 묶음까지만 옮기고, 다시 조회해 옮기면 남은 것만 이어서 옮긴다", async () => {
    const uid = uniqueId("user");
    const count = REPORT_MOVE_BATCH_SIZE + 10;
    const docs = await seedAndQuery(uid, count);
    // 두 번째 묶음에 들어갈 문서 하나를 조회 뒤에 바꿈
    await docs[REPORT_MOVE_BATCH_SIZE + 5].ref.update({status: "resolved"});

    await assert.rejects(moveReportsAnonymously(db, docs));

    const firstCopies = [...(await countCopiesByTarget(uid)).values()];
    assert.strictEqual(
        firstCopies.reduce((a, b) => a + b, 0), REPORT_MOVE_BATCH_SIZE,
    );
    assert.strictEqual(await countOriginals(uid), 10);

    const requeried = (await reports.where("reporterId", "==", uid).get()).docs;
    await moveReportsAnonymously(db, requeried);

    assert.strictEqual(await countOriginals(uid), 0);
    const copies = await countCopiesByTarget(uid);
    assert.strictEqual(copies.size, count);
    assert.ok([...copies.values()].every((n) => n === 1), "대상마다 사본 1개");
  });
});
