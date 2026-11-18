// grantStudyReward 트리거 통합 테스트 (#620)
// 에뮬레이터에서 실행된다: npm --prefix functions test
// 1차 PR에서는 트리거 대기 헬퍼(waitForDoc) 동작 확인용 1건만 둔다. 나머지 케이스는 2차 PR에서 채운다
const {describe, it, before} = require("node:test");
const assert = require("node:assert");
const {
  clearFirestore, db, uniqueId, waitForDoc,
} = require("../helpers/emulator");

const HOUR_MS = 60 * 60 * 1000;

describe("grantStudyReward", () => {
  before(async () => {
    await clearFirestore();
  });

  it("pending 기록이 생기면 아이템·박스를 지급하고 granted로 바꾼다", async () => {
    const uid = uniqueId("user");
    const userRef = db.collection("users").doc(uid);
    await userRef.set({item: {heart: 2, box: 0}});
    const logRef = userRef.collection("pots").doc("pot1")
        .collection("logs").doc(uniqueId("log"));

    await logRef.set({studyingTime: HOUR_MS, rewardStatus: "pending"});

    const log = await waitForDoc(
        logRef, (data) => data && data.rewardStatus !== "pending",
    );
    assert.strictEqual(log.rewardStatus, "granted");
    assert.deepStrictEqual(log.reward, {items: ["heart"], box: 1});
    const user = (await userRef.get()).data();
    assert.deepStrictEqual(user.item, {heart: 3, box: 1});
  });
});
