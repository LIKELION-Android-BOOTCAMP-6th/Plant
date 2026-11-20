// retryPendingStudyRewards(지급 실패로 남은 pending 학습 보상 보정) 통합 테스트 (#609)
// 에뮬레이터에서 실행된다: npm --prefix functions test
// - 예약 함수는 에뮬레이터에서 정해진 시각에 실행되지 않으므로, studyReward.js의 함수를 직접 부른다
// - 문서의 서버 생성 시각(createTime)은 과거 값으로 만들 수 없어 기준 시각(nowMs)을 미래로 넣는다
// - (default) DB에 쓰면 트리거가 바로 지급해 "pending으로 남은 기록"을 만들 수 없다
//   → 트리거가 붙지 않는 DB(getTriggerFreeDb)에서 확인한다
// - 트리거와 겹치는 경우는 실제 트리거로 확인하지 않는다
//   트리거는 pending이 아닌 기록을 만나면 아무것도 남기지 않고 끝나 실행이 끝났는지 알 수 없고,
//   에뮬레이터에서는 트리거가 테스트보다 늦게 시작돼 확인 전에 실행되지 않았다
//   → 트리거와 보정은 같은 grantStudyReward를 부르므로 "보정이 동시에 두 번" 테스트로 확인한다
const {describe, it, beforeEach} = require("node:test");
const assert = require("node:assert");
const logger = require("firebase-functions/logger");
const {
  PENDING_REWARD_MIN_AGE_MS, grantStudyReward, retryPendingStudyRewards,
} = require("../../studyReward");
const {
  TRIGGER_FREE_DATABASE_ID, clearFirestore, getTriggerFreeDb, uniqueId,
} = require("../helpers/emulator");

const retryDb = getTriggerFreeDb();

const HOUR_MS = 60 * 60 * 1000;
// 방금 만든 기록도 "25시간 넘게 지난 기록"으로 보이게 하는 기준 시각
const futureNowMs = () => Date.now() + PENDING_REWARD_MIN_AGE_MS + HOUR_MS;

/**
 * 유저 문서와 pending 학습 기록을 만든다.
 * @param {Object} [options]
 * @param {number} [options.studyingTime] 학습 시간(ms). 기본 1시간(하트 1 + 박스 1)
 * @param {boolean} [options.withUser] false면 유저 문서를 만들지 않는다 (탈퇴 상태)
 * @param {FirebaseFirestore.DocumentReference} [options.userRef]
 *     이미 만든 유저에 기록을 더할 때 넘긴다 (유저 문서는 다시 쓰지 않음)
 * @return {Promise<{userRef: FirebaseFirestore.DocumentReference,
 *     logRef: FirebaseFirestore.DocumentReference, createTimeMs: number}>}
 */
async function seedPendingLog({
  studyingTime = HOUR_MS, withUser = true,
  userRef: existingUserRef,
} = {}) {
  const userRef = existingUserRef ||
    retryDb.collection("users").doc(uniqueId("user"));
  if (withUser && !existingUserRef) {
    await userRef.set({item: {heart: 0, box: 0}});
  }
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

  // 트리거와 보정이 같은 기록을 동시에 처리하는 경우도 이 테스트로 본다 (둘 다 grantStudyReward)
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

  it("실패 원인(오류)을 로그의 별도 인자로 남긴다", async () => {
    const broken = await seedPendingLog();
    const failure = new Error("테스트용 지급 실패");
    const grant = async () => {
      throw failure;
    };
    // 객체 안에 넣으면 JSON으로 바뀌며 message·stack이 빠진다 (#711)
    const calls = [];
    const originalError = logger.error;
    logger.error = (...args) => calls.push(args);
    try {
      await retryPendingStudyRewards(retryDb, futureNowMs(), {grant});
    } finally {
      logger.error = originalError;
    }

    assert.strictEqual(calls.length, 1);
    const [message, error, context] = calls[0];
    assert.strictEqual(typeof message, "string");
    assert.strictEqual(error, failure);
    // 마지막 인자(plain object)가 로그의 구조화 필드가 된다
    assert.deepStrictEqual(context, {path: broken.logRef.path});
  });

  it("같은 유저의 한 기록이 실패해도 그 유저의 나머지 기록은 지급한다", async () => {
    const broken = await seedPendingLog();
    const {userRef} = broken;
    const okLogs = [];
    for (let i = 0; i < 2; i++) okLogs.push(await seedPendingLog({userRef}));
    const brokenLogId = broken.logRef.id;
    const grant = async (database, uid, potId, logId) => {
      if (logId === brokenLogId) throw new Error("테스트용 지급 실패");
      return grantStudyReward(database, uid, potId, logId);
    };

    const result = await retryPendingStudyRewards(
        retryDb, futureNowMs(), {grant},
    );

    assert.strictEqual(result.retried, 2);
    assert.strictEqual(result.failed, 1);
    assert.strictEqual((await read(broken.logRef)).rewardStatus, "pending");
    for (const {logRef} of okLogs) {
      assert.strictEqual((await read(logRef)).rewardStatus, "granted");
    }
    assert.deepStrictEqual((await read(userRef)).item, {heart: 2, box: 2});
  });

  it("같은 유저의 기록은 차례로, 다른 유저끼리는 동시에 지급한다", async () => {
    const USERS = 3;
    const LOGS_PER_USER = 3;
    const userRefs = [];
    for (let u = 0; u < USERS; u++) {
      const {userRef} = await seedPendingLog();
      for (let i = 1; i < LOGS_PER_USER; i++) await seedPendingLog({userRef});
      userRefs.push(userRef);
    }
    // 시간이 아니라 "지금 실행 중인 지급 수"를 세어 확인한다
    const activeByUid = new Map();
    let active = 0;
    let maxActive = 0;
    let maxActiveSameUid = 0;
    const grant = async (database, uid, potId, logId) => {
      const sameUid = (activeByUid.get(uid) || 0) + 1;
      activeByUid.set(uid, sameUid);
      active++;
      maxActive = Math.max(maxActive, active);
      maxActiveSameUid = Math.max(maxActiveSameUid, sameUid);
      try {
        return await grantStudyReward(database, uid, potId, logId);
      } finally {
        activeByUid.set(uid, activeByUid.get(uid) - 1);
        active--;
      }
    };

    const result = await retryPendingStudyRewards(
        retryDb, futureNowMs(), {grant},
    );

    assert.strictEqual(result.retried, USERS * LOGS_PER_USER);
    assert.strictEqual(maxActiveSameUid, 1);
    assert.strictEqual(maxActive, USERS);
    for (const userRef of userRefs) {
      assert.deepStrictEqual(
          (await read(userRef)).item,
          {heart: LOGS_PER_USER, box: LOGS_PER_USER},
      );
    }
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
