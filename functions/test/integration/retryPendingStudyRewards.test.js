// retryPendingStudyRewards(지급 실패로 남은 pending 학습 보상 보정) 통합 테스트 (#609)
// 에뮬레이터에서 실행된다: npm --prefix functions test
// - 예약 함수는 에뮬레이터에서 정해진 시각에 실행되지 않으므로, studyReward.js의 함수를 직접 부른다
// - 문서의 서버 생성 시각(createTime)은 과거 값으로 만들 수 없어 기준 시각(nowMs)을 미래로 넣는다
// - (default) DB에 쓰면 트리거가 바로 지급해 "pending으로 남은 기록"을 만들 수 없다
//   → 트리거가 붙지 않는 DB(getTriggerFreeDb)에서 확인한다
//   트리거와 겹치는 경우만 (default) DB에서 실제 트리거와 함께 실행한다
const {describe, it, beforeEach} = require("node:test");
const assert = require("node:assert");
const {
  PENDING_REWARD_MIN_AGE_MS, grantStudyReward, retryPendingStudyRewards,
} = require("../../studyReward");
const {
  TRIGGER_FREE_DATABASE_ID, clearFirestore, db, getTriggerFreeDb, uniqueId,
  waitForDoc,
} = require("../helpers/emulator");

const retryDb = getTriggerFreeDb();

const HOUR_MS = 60 * 60 * 1000;
// 방금 만든 기록도 "25시간 넘게 지난 기록"으로 보이게 하는 기준 시각
const futureNowMs = () => Date.now() + PENDING_REWARD_MIN_AGE_MS + HOUR_MS;

/**
 * 유저 문서와 pending 학습 기록을 만든다.
 * @param {Object} [options]
 * @param {FirebaseFirestore.Firestore} [options.database] 기본은 트리거가 붙지 않는 DB
 * @param {number} [options.studyingTime] 학습 시간(ms). 기본 1시간(하트 1 + 박스 1)
 * @param {boolean} [options.withUser] false면 유저 문서를 만들지 않는다 (탈퇴 상태)
 * @return {Promise<{userRef: FirebaseFirestore.DocumentReference,
 *     logRef: FirebaseFirestore.DocumentReference, createTimeMs: number}>}
 */
async function seedPendingLog({
  database = retryDb, studyingTime = HOUR_MS, withUser = true,
} = {}) {
  const userRef = database.collection("users").doc(uniqueId("user"));
  if (withUser) await userRef.set({item: {heart: 0, box: 0}});
  const logRef = userRef.collection("pots").doc(uniqueId("pot"))
      .collection("logs").doc(uniqueId("log"));
  await logRef.set({studyingTime, rewardStatus: "pending"});
  const createTimeMs = (await logRef.get()).createTime.toMillis();
  return {userRef, logRef, createTimeMs};
}

/**
 * @param {FirebaseFirestore.DocumentReference} ref
 * @return {Promise<Object|undefined>}
 */
async function read(ref) {
  return (await ref.get()).data();
}

describe("retryPendingStudyRewards", () => {
  beforeEach(async () => {
    await clearFirestore(TRIGGER_FREE_DATABASE_ID);
  });

  it("pending 기록이 없으면 0건으로 끝난다", async () => {
    const result = await retryPendingStudyRewards(retryDb, futureNowMs());
    assert.deepStrictEqual(result, {
      scanned: 0, skippedRecent: 0, skippedPath: 0,
      retried: 0, failed: 0, hasMore: false,
    });
  });

  it("25시간 넘은 pending 기록을 지급한다 (1시간 이상 granted, 미만 none)", async () => {
    const granted = await seedPendingLog({studyingTime: HOUR_MS});
    const none = await seedPendingLog({studyingTime: HOUR_MS - 1});

    const result = await retryPendingStudyRewards(retryDb, futureNowMs());

    assert.strictEqual(result.retried, 2);
    assert.strictEqual(result.failed, 0);
    const grantedLog = await read(granted.logRef);
    assert.strictEqual(grantedLog.rewardStatus, "granted");
    assert.deepStrictEqual(grantedLog.reward, {items: ["heart"], box: 1});
    assert.deepStrictEqual(
        (await read(granted.userRef)).item, {heart: 1, box: 1},
    );
    assert.strictEqual((await read(none.logRef)).rewardStatus, "none");
    assert.deepStrictEqual((await read(none.userRef)).item, {heart: 0, box: 0});
  });

  it("서버 생성 후 25시간이 안 된 기록은 건드리지 않는다 (경계값)", async () => {
    const {userRef, logRef, createTimeMs} = await seedPendingLog();

    const early = await retryPendingStudyRewards(
        retryDb, createTimeMs + PENDING_REWARD_MIN_AGE_MS - 1,
    );
    assert.strictEqual(early.scanned, 1);
    assert.strictEqual(early.skippedRecent, 1);
    assert.strictEqual(early.retried, 0);
    assert.strictEqual((await read(logRef)).rewardStatus, "pending");
    assert.deepStrictEqual((await read(userRef)).item, {heart: 0, box: 0});

    // 정확히 25시간이 되면 처리한다
    const onTime = await retryPendingStudyRewards(
        retryDb, createTimeMs + PENDING_REWARD_MIN_AGE_MS,
    );
    assert.strictEqual(onTime.retried, 1);
    assert.strictEqual((await read(logRef)).rewardStatus, "granted");
  });

  it("보정이 동시에 두 번 실행돼도 1번만 지급한다", async () => {
    const {userRef, logRef} = await seedPendingLog();
    const nowMs = futureNowMs();

    await Promise.all([
      retryPendingStudyRewards(retryDb, nowMs),
      retryPendingStudyRewards(retryDb, nowMs),
    ]);

    assert.strictEqual((await read(logRef)).rewardStatus, "granted");
    assert.deepStrictEqual((await read(userRef)).item, {heart: 1, box: 1});
  });

  it("트리거와 겹쳐 실행돼도 1번만 지급한다", async () => {
    // (default) DB에 기록을 만들고 바로 보정을 실행해, 트리거와 보정이 같은 기록을 처리하게 한다
    const {userRef, logRef} = await seedPendingLog({database: db});
    const result = await retryPendingStudyRewards(db, futureNowMs());
    assert.strictEqual(result.failed, 0);
    await waitForDoc(
        logRef, (data) => data && data.rewardStatus === "granted",
    );
    // 지급과 granted 변경은 한 트랜잭션이고, 둘 다 pending일 때만 쓴다 → granted가 보인 뒤에는
    // 늦게 끝나는 쪽이 있어도 pending이 아니라서 더 지급하지 않는다
    assert.deepStrictEqual((await read(userRef)).item, {heart: 1, box: 1});
  });

  it("유저 문서가 없으면(탈퇴) 지급하지 않고 실패로 세지 않는다", async () => {
    const {userRef, logRef} = await seedPendingLog({withUser: false});

    const result = await retryPendingStudyRewards(retryDb, futureNowMs());

    assert.strictEqual(result.retried, 1);
    assert.strictEqual(result.failed, 0);
    assert.strictEqual((await read(logRef)).rewardStatus, "pending");
    // 지워진 유저 문서를 다시 만들지 않는다
    assert.strictEqual((await userRef.get()).exists, false);
  });

  it("조회한 뒤 처리 전에 기록이 지워지면 건너뛴다", async () => {
    const {userRef, logRef} = await seedPendingLog();
    // 조회와 지급 사이에 기록이 지워진 상황을 만든다
    const grant = async (...args) => {
      await logRef.delete();
      return grantStudyReward(...args);
    };

    const result = await retryPendingStudyRewards(
        retryDb, futureNowMs(), {grant},
    );

    assert.strictEqual(result.retried, 1);
    assert.strictEqual(result.failed, 0);
    assert.strictEqual((await logRef.get()).exists, false);
    assert.deepStrictEqual((await read(userRef)).item, {heart: 0, box: 0});
  });

  it("한 기록이 실패해도 나머지는 지급하고, 실패한 기록은 다음 실행에서 다시 시도한다", async () => {
    const broken = await seedPendingLog();
    const ok = await seedPendingLog();
    const brokenLogId = broken.logRef.id;
    let brokenCalls = 0;
    const grant = async (database, uid, potId, logId) => {
      if (logId === brokenLogId) {
        brokenCalls++;
        throw new Error("테스트용 지급 실패");
      }
      return grantStudyReward(database, uid, potId, logId);
    };

    const first = await retryPendingStudyRewards(
        retryDb, futureNowMs(), {grant},
    );
    assert.strictEqual(first.retried, 1);
    assert.strictEqual(first.failed, 1);
    assert.strictEqual((await read(ok.logRef)).rewardStatus, "granted");
    assert.strictEqual((await read(broken.logRef)).rewardStatus, "pending");

    const second = await retryPendingStudyRewards(
        retryDb, futureNowMs(), {grant},
    );
    assert.strictEqual(second.scanned, 1);
    assert.strictEqual(second.failed, 1);
    assert.strictEqual(brokenCalls, 2);
  });

  it("한 번에 maxPerRun개까지만 지급하고 남은 기록은 다음 실행에서 처리한다", async () => {
    const seeds = [];
    for (let i = 0; i < 5; i++) seeds.push(await seedPendingLog());
    const options = {pageSize: 2, maxPerRun: 2};
    const nowMs = futureNowMs();

    const runs = [];
    for (let i = 0; i < 3; i++) {
      runs.push(await retryPendingStudyRewards(retryDb, nowMs, options));
    }

    assert.deepStrictEqual(
        runs.map((run) => [run.retried, run.hasMore]),
        [[2, true], [2, true], [1, false]],
    );
    for (const {logRef, userRef} of seeds) {
      assert.strictEqual((await read(logRef)).rewardStatus, "granted");
      assert.deepStrictEqual((await read(userRef)).item, {heart: 1, box: 1});
    }
  });

  it("maxScan개를 훑으면 멈추고 다음 실행으로 넘긴다", async () => {
    for (let i = 0; i < 3; i++) await seedPendingLog();

    const result = await retryPendingStudyRewards(
        retryDb, futureNowMs(), {pageSize: 2, maxScan: 2},
    );

    assert.strictEqual(result.scanned, 2);
    assert.strictEqual(result.retried, 2);
    assert.strictEqual(result.hasMore, true);
  });

  it("학습 기록 경로가 아닌 logs 컬렉션의 문서는 건너뛴다", async () => {
    const otherRef = retryDb.collection("other").doc(uniqueId("doc"))
        .collection("logs").doc(uniqueId("log"));
    await otherRef.set({studyingTime: HOUR_MS, rewardStatus: "pending"});

    const result = await retryPendingStudyRewards(retryDb, futureNowMs());

    assert.strictEqual(result.skippedPath, 1);
    assert.strictEqual(result.retried, 0);
    assert.strictEqual((await read(otherRef)).rewardStatus, "pending");
  });
});
